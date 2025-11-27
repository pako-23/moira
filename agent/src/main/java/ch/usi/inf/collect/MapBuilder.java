package ch.usi.inf.collect;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class MapBuilder<K, V> {
  private static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;
  private static final float DEFAULT_LOAD_FACTOR = 0.75f;

  private KeyDeletionCallback<V> keyDeletionCallback;
  private Equivalence<K> equivalence;
  private HashFunction<K> hashFunction;
  private int initialCapacity;
  private ReferenceStrength keyReferenceStrength;
  private float loadFactor;

  private MapBuilder() {
    keyDeletionCallback =
        (value) -> {
          return;
        };
    equivalence = (first, second) -> first.equals(second);
    hashFunction = (key) -> key.hashCode();
    initialCapacity = DEFAULT_INITIAL_CAPACITY;
    keyReferenceStrength = ReferenceStrength.STRONG;
    loadFactor = DEFAULT_LOAD_FACTOR;
  }

  public static <K, V> MapBuilder<K, V> builder() {
    return new MapBuilder<K, V>();
  }

  public MapBuilder<K, V> initialCapacity(int initialCapacity) {
    if (initialCapacity <= 0 || (initialCapacity & (initialCapacity - 1)) != 0)
      throw new IllegalArgumentException("The initial capacity must be a power of two");

    this.initialCapacity = initialCapacity;
    return this;
  }

  public MapBuilder<K, V> weakKeys() {
    this.keyReferenceStrength = ReferenceStrength.WEAK;
    return this;
  }

  public MapBuilder<K, V> strongKeys() {
    this.keyReferenceStrength = ReferenceStrength.STRONG;
    return this;
  }

  public MapBuilder<K, V> hashFunction(HashFunction<K> hashFunction) {
    this.hashFunction = hashFunction;
    return this;
  }

  public MapBuilder<K, V> equivalence(Equivalence<K> equivalence) {
    this.equivalence = equivalence;
    return this;
  }

  public MapBuilder<K, V> keyDeletionCallback(KeyDeletionCallback<V> callback) {
    this.keyDeletionCallback = callback;
    return this;
  }

  public MapBuilder<K, V> loadFactor(final float loadFactor) {
    this.loadFactor = loadFactor;
    return this;
  }

  public float getLoadFactor() {
    return loadFactor;
  }

  public int getInitialCapacity() {
    return initialCapacity;
  }

  public Equivalence<K> getEquivalence() {
    return equivalence;
  }

  public HashFunction<K> getHashFunction() {
    return hashFunction;
  }

  public KeyDeletionCallback<V> getKeyDeletionCallback() {
    return keyDeletionCallback;
  }

  public ReferenceStrength getKeyReferenceStrength() {
    return keyReferenceStrength;
  }

  public Map<K, V> build() {
    return new HashMap(this);
  }

  @FunctionalInterface
  public interface KeyDeletionCallback<V> {
    public void apply(V value);
  }

  private interface Entry<K, V> {
    public Entry<K, V> getNext();

    public K getKey();

    public V getValue();

    public int getHash();

    public void setNext(final Entry<K, V> next);

    public void setValue(final V value);
  }

  private static class StrongEntry<K, V> implements Entry<K, V> {
    private final K key;
    private final int hash;
    private V value;
    private Entry<K, V> next;

    public StrongEntry(final K key, final int hash) {
      this.key = key;
      this.value = null;
      this.hash = hash;
      this.next = null;
    }

    @Override
    public Entry<K, V> getNext() {
      return next;
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public int getHash() {
      return hash;
    }

    @Override
    public void setNext(final Entry<K, V> next) {
      this.next = next;
    }

    @Override
    public void setValue(final V value) {
      this.value = value;
    }
  }

  private static class WeakEntry<K, V> extends WeakReference<Object> implements Entry<K, V> {
    private final int hash;
    private V value;
    private Entry<K, V> next;

    public WeakEntry(final K key, final int hash, final ReferenceQueue<Object> queue) {
      super(key, queue);
      this.hash = hash;
      this.value = null;
      this.next = null;
    }

    @Override
    public Entry<K, V> getNext() {
      return next;
    }

    @Override
    @SuppressWarnings("unchecked")
    public K getKey() {
      return (K) get();
    }

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public int getHash() {
      return hash;
    }

    @Override
    public void setNext(final Entry<K, V> next) {
      this.next = next;
    }

    @Override
    public void setValue(final V value) {
      this.value = value;
    }
  }

  public enum ReferenceStrength {
    STRONG {
      @Override
      public <K, V> Entry<K, V> create(
          final K key, final int hash, final ReferenceQueue<Object> queue) {
        return new StrongEntry<K, V>(key, hash);
      }
    },
    WEAK {
      @Override
      public <K, V> Entry<K, V> create(
          final K key, final int hash, final ReferenceQueue<Object> queue) {
        return new WeakEntry<K, V>(key, hash, queue);
      }
    };

    public abstract <K, V> Entry<K, V> create(
        final K key, final int hash, final ReferenceQueue<Object> queue);
  }

  private class HashMap implements Map<K, V> {
    private int size;
    private final float loadFactor;
    private int threshold;
    private Entry<K, V>[] table;
    private final KeyDeletionCallback<V> keyDeletionCallback;
    private final HashFunction<K> hashFunction;
    private final Equivalence<K> equivalence;
    private final ReferenceStrength referenceStrength;
    private final ReferenceQueue<Object> queue;

    public HashMap(final MapBuilder<K, V> builder) {
      int capacity = builder.getInitialCapacity();
      size = 0;
      loadFactor = builder.getLoadFactor();
      threshold = (int) (capacity * loadFactor);
      table = newTable(builder.getInitialCapacity());
      keyDeletionCallback = builder.getKeyDeletionCallback();
      hashFunction = builder.getHashFunction();
      equivalence = builder.getEquivalence();
      referenceStrength = builder.getKeyReferenceStrength();
      queue = referenceStrength == ReferenceStrength.STRONG ? null : new ReferenceQueue<>();
    }

    @Override
    public boolean contains(final K key) {
      int hash = hash(key);

      return search(key, hash) != null;
    }

    private Entry<K, V> search(final K key, final int hash) {
      int index = indexFor(hash, table.length);

      for (Entry<K, V> node = table[index]; node != null; node = node.getNext()) {
        if (node.getKey() != null
            && node.getHash() == hash
            && equivalence.test(node.getKey(), key)) {
          return node;
        }
      }

      return null;
    }

    @Override
    public V get(final K key) {
      final Entry<K, V> node = search(key, hash(key));
      if (node == null) return null;
      return node.getValue();
    }

    @Override
    public V getOrPut(final K key, final ValueProducer<V> producer) {
      int hash = hash(key);
      Entry<K, V> node = search(key, hash);
      if (node != null) return node.getValue();

      reclaim();
      int index = indexFor(hash, table.length);
      node = referenceStrength.create(key, hash, queue);
      node.setNext(table[index]);
      node.setValue(producer.produce());
      table[index] = node;

      if (++size > threshold) rehash();

      return node.getValue();
    }

    @Override
    public int capacity() {
      return table.length;
    }

    @Override
    public int size() {
      reclaim();
      return size;
    }

    private void rehash() {
      int length = table.length << 1;
      final Entry<K, V>[] table = this.table;
      this.threshold = (int) (length * loadFactor);
      this.table = newTable(length);
      transfer(table, this.table);
    }

    private void transfer(final Entry<K, V>[] source, final Entry<K, V>[] destination) {
      for (int i = 0; i < source.length; ++i) {
        Entry<K, V> node = source[i];
        source[i] = null;
        while (node != null) {
          Entry<K, V> next = node.getNext();
          if (node.getKey() == null) {
            keyDeletionCallback.apply(node.getValue());
            node.setNext(null);
            node.setValue(null);
            --size;
          } else {
            int index = indexFor(node.getHash(), destination.length);
            node.setNext(destination[index]);
            destination[index] = node;
          }
          node = next;
        }
      }
    }

    private int indexFor(final int hash, final int length) {
      return hash & (length - 1);
    }

    private int hash(final K key) {
      int hash = hashFunction.compute(key);
      hash ^= (hash >>> 20) ^ (hash >>> 12);
      return hash ^ (hash >>> 7) ^ (hash >>> 4);
    }

    @SuppressWarnings("unchecked")
    private Entry<K, V>[] newTable(final int length) {
      return (Entry<K, V>[]) new Entry[length];
    }

    @SuppressWarnings("unchecked")
    private Entry<K, V> pollEntry() {
      return (Entry<K, V>) queue.poll();
    }

    private void reclaim() {
      if (queue == null) return;

      while (true) {
        Entry<K, V> node = pollEntry();
        if (node == null) break;
        if (node.getValue() == null) continue;
        keyDeletionCallback.apply(node.getValue());
        int index = indexFor(node.getHash(), table.length);
        Entry<K, V> prev = table[index];
        Entry<K, V> p = prev;
        while (p != null) {
          Entry<K, V> next = p.getNext();
          if (p == node) {
            if (prev == node) table[index] = next;
            else prev.setNext(next);
            node.setValue(null);
            --size;
            break;
          }
          prev = p;
          p = next;
        }
      }
    }

    @Override
    public Map.Iterator<K, V> iterator() {
      return new Iterator();
    }

    private class Iterator implements Map.Iterator<K, V> {
      private int index;
      private Entry<K, V> node;

      public Iterator() {
        index = 0;
        advance();
      }

      private void advance() {
        while (hasNext() && table[index] == null) ++index;

        if (hasNext()) node = table[index];
      }

      @Override
      public boolean hasNext() {
        return index < table.length;
      }

      @Override
      public V value() {
        return node.getValue();
      }

      @Override
      public K key() {
        return node.getKey();
      }

      @Override
      public void next() {
        node = node.getNext();
        if (node != null) return;

        ++index;
        advance();
      }
    }
  }
}

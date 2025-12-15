package moira.collect;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class MapBuilder<K, V> {
  private static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;
  private static final float DEFAULT_LOAD_FACTOR = 0.5f;

  private KeyDeletionCallback<V> keyDeletionCallback;
  private HashFunction<K> hashFunction;
  private int initialCapacity;
  private ReferenceStrength keyReferenceStrength;
  private float loadFactor;

  private MapBuilder() {
    keyDeletionCallback =
        (value) -> {
          return;
        };
    hashFunction = (key) -> key.hashCode();
    initialCapacity = DEFAULT_INITIAL_CAPACITY;
    keyReferenceStrength = ReferenceStrength.STRONG;
    loadFactor = DEFAULT_LOAD_FACTOR;
  }

  public static <K, V> MapBuilder<K, V> builder() {
    return new MapBuilder<K, V>();
  }

  public MapBuilder<K, V> initialCapacity(final int initialCapacity) {
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

  public MapBuilder<K, V> hashFunction(final HashFunction<K> hashFunction) {
    this.hashFunction = hashFunction;
    return this;
  }

  public MapBuilder<K, V> keyDeletionCallback(final KeyDeletionCallback<V> callback) {
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
    return new HashMap<>(this);
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

  private static final class StrongEntry<K, V> implements Entry<K, V> {
    private final K key;
    private final int hash;
    private volatile V value;
    private volatile Entry<K, V> next;

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

  private static final class WeakEntry<K, V> extends WeakReference<Object> implements Entry<K, V> {
    private final int hash;
    private volatile V value;
    private volatile Entry<K, V> next;

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
      public <K> Equivalence<K> defaultEquivalence() {
        return (first, second) -> first.equals(second);
      }
    },
    WEAK {
      @Override
      public <K> Equivalence<K> defaultEquivalence() {
        return (first, second) -> first == second;
      }
    };

    public abstract <K> Equivalence<K> defaultEquivalence();
  }

  private static final class HashMap<K, V> implements Map<K, V> {
    private abstract static class Segment<K, V> {
      protected Entry<K, V>[] table;
      protected int size;
      private final float loadFactor;
      private int threshold;
      private final Equivalence<K> equivalence;
      protected final KeyDeletionCallback<V> keyDeletionCallback;

      public Segment(final MapBuilder<K, V> builder, int capacity) {
        table = newTable(capacity);
        size = 0;
        loadFactor = builder.getLoadFactor();
        equivalence = builder.getKeyReferenceStrength().defaultEquivalence();
        threshold = (int) (capacity * loadFactor);
        keyDeletionCallback = builder.getKeyDeletionCallback();
      }

      public boolean contains(final K key, final int hash) {
        return search(key, hash) != null;
      }

      public V get(final K key, final int hash) {
        final Entry<K, V> entry = search(key, hash);
        if (entry == null) return null;
        return entry.getValue();
      }

      public int capacity() {
        return table.length;
      }

      public int size() {
        return size;
      }

      public V getOrPut(final K key, final int hash, final ValueProducer<V> producer) {
        Entry<K, V> entry = search(key, hash);
        if (entry != null) return entry.getValue();

        reclaim();

        int index = indexFor(hash, table.length);
        entry = newEntry(key, hash);
        entry.setNext(table[index]);
        entry.setValue(producer.produce());
        table[index] = entry;

        if (++size > threshold) rehash();

        return entry.getValue();
      }

      protected abstract Entry<K, V> newEntry(final K key, final int hash);

      protected void reclaim() {}

      private Entry<K, V> search(final K key, final int hash) {
        final Entry<K, V>[] table = this.table;
        final int index = indexFor(hash, table.length);

        for (Entry<K, V> entry = table[index]; entry != null; entry = entry.getNext()) {
          if (entry.getKey() != null
              && entry.getHash() == hash
              && equivalence.test(entry.getKey(), key)) {
            return entry;
          }
        }

        return null;
      }

      private void rehash() {
        final int capacity = table.length << 1;
        final Entry<K, V>[] table = this.table;
        this.threshold = (int) (capacity * loadFactor);
        this.table = newTable(capacity);
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

      @SuppressWarnings("unchecked")
      private Entry<K, V>[] newTable(final int capacity) {
        return (Entry<K, V>[]) new Entry[capacity];
      }

      protected int indexFor(final int hash, final int capacity) {
        return hash & (capacity - 1);
      }
    }

    private static final class WeakSegment<K, V> extends Segment<K, V> {
      private static final int MAX_RECLAIM_BACKOFF = 64;
      private final ReferenceQueue<Object> queue;
      private int reclaimInvocations;
      private int reclaimThreshold;

      public WeakSegment(final MapBuilder<K, V> builder, int capacity) {
        super(builder, capacity);
        queue = new ReferenceQueue<>();
        reclaimInvocations = 0;
        reclaimThreshold = 1;
      }

      @Override
      protected Entry<K, V> newEntry(final K key, final int hash) {
        return new WeakEntry<>(key, hash, queue);
      }

      @SuppressWarnings("unchecked")
      private Entry<K, V> pollEntry() {
        return (Entry<K, V>) queue.poll();
      }

      @Override
      protected void reclaim() {
        if (++reclaimInvocations < reclaimThreshold) return;

        Entry<K, V> entry = pollEntry();
        if (entry == null) {
          reclaimThreshold <<= 1;
          if (reclaimThreshold > MAX_RECLAIM_BACKOFF) reclaimThreshold = MAX_RECLAIM_BACKOFF;
          return;
        }

        reclaimThreshold = 1;
        reclaimInvocations = 0;
        do {
          if (entry.getValue() == null) continue;
          keyDeletionCallback.apply(entry.getValue());
          int index = indexFor(entry.getHash(), table.length);
          Entry<K, V> prev = table[index];
          Entry<K, V> p = prev;
          while (true) {
            Entry<K, V> next = p.getNext();
            if (p == entry) {
              if (prev == entry) table[index] = next;
              else prev.setNext(next);
              entry.setValue(null);
              --size;
              break;
            }
            prev = p;
            p = next;
          }
        } while ((entry = pollEntry()) != null);
      }
    }

    private static final class StrongSegment<K, V> extends Segment<K, V> {
      public StrongSegment(final MapBuilder<K, V> builder, int capacity) {
        super(builder, capacity);
      }

      @Override
      protected Entry<K, V> newEntry(final K key, final int hash) {
        return new StrongEntry<>(key, hash);
      }
    }

    private final HashFunction<K> hashFunction;
    private final Segment<K, V> map;

    public HashMap(final MapBuilder<K, V> builder) {
      hashFunction = builder.getHashFunction();
      map = newSegment(builder, builder.getInitialCapacity());
    }

    @Override
    public boolean contains(final K key) {
      int hash = hash(key);

      return map.contains(key, hash);
    }

    @Override
    public V get(final K key) {
      int hash = hash(key);

      return map.get(key, hash);
    }

    @Override
    public V getOrPut(final K key, final ValueProducer<V> producer) {
      int hash = hash(key);

      return map.getOrPut(key, hash, producer);
    }

    @Override
    public int capacity() {
      return map.capacity();
    }

    @Override
    public int size() {
      return map.size();
    }

    private final int hash(final K key) {
      int h = hashFunction.compute(key);
      h ^= (h >>> 20) ^ (h >>> 12);
      return h ^ (h >>> 7) ^ (h >>> 4);
    }

    private Segment<K, V> newSegment(final MapBuilder<K, V> builder, final int capacity) {
      switch (builder.getKeyReferenceStrength()) {
        case WEAK:
          return new WeakSegment<>(builder, capacity);
        default:
          return new StrongSegment<>(builder, capacity);
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
        while (hasNext() && map.table[index] == null) ++index;
        if (hasNext()) node = map.table[index];
      }

      @Override
      public boolean hasNext() {
        return index < map.capacity();
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

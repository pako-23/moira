package ch.usi.inf.profiler;

public class MapBuilder<K, V> {
  private static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;

  private KeyDeletionCallback<V> keyDeletionCallback;
  private Equivalence<K> equivalence;
  private HashFunction<K> hashFunction;
  private int initialCapacity;
  private ReferenceStrength keyReferenceStrength;

  private MapBuilder() {
    keyDeletionCallback =
        (value) -> {
          return;
        };
    equivalence = (first, second) -> first.equals(second);
    hashFunction = (key) -> key.hashCode();
    initialCapacity = DEFAULT_INITIAL_CAPACITY;
    keyReferenceStrength = ReferenceStrength.STRONG;
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

  private interface Reference<K> {
    public K get();
  }

  private static class StrongReference<K> implements Reference<K> {
    private final K reference;

    public StrongReference(K reference) {
      this.reference = reference;
    }

    @Override
    public K get() {
      return reference;
    }
  }

  private static class WeakReference<K> implements Reference<K> {
    private final java.lang.ref.WeakReference<K> reference;

    public WeakReference(K reference) {
      this.reference = new java.lang.ref.WeakReference<>(reference);
    }

    @Override
    public K get() {
      return reference.get();
    }
  }

  public enum ReferenceStrength {
    STRONG {
      public <K> Reference<K> create(K object) {
        return new StrongReference<K>(object);
      }
    },
    WEAK {
      public <K> Reference<K> create(K object) {
        return new WeakReference<K>(object);
      }
    };

    public abstract <K> Reference<K> create(K object);
  }

  private class HashMap implements Map<K, V> {
    private int capacity;
    private int size;
    private Reference<K>[] keys;
    private V[] values;
    private int[] hashes;
    private final KeyDeletionCallback<V> keyDeletionCallback;
    private final HashFunction<K> hashFunction;
    private final Equivalence<K> equivalence;
    private final ReferenceStrength keyReferenceStrength;

    public HashMap(final MapBuilder<K, V> builder) {
      capacity = builder.getInitialCapacity();
      size = 0;
      keys = newKeys(capacity);
      values = newValues(capacity);
      hashes = new int[capacity];
      keyDeletionCallback = builder.getKeyDeletionCallback();
      hashFunction = builder.getHashFunction();
      equivalence = builder.getEquivalence();
      keyReferenceStrength = builder.getKeyReferenceStrength();
    }

    @Override
    public boolean contains(final K key) {
      int hash = hashFunction.compute(key);
      int index = keyIndex(key, hash);

      return keys[index] != null && keys[index].get() != null;
    }

    @Override
    public V getOrPut(final K key, final ValueProducer<V> producer) {
      int hash = hashFunction.compute(key);
      int index = keyIndex(key, hash);

      if (keys[index] == null) {
        if (2 * size >= capacity && rehash()) {
          index = keyIndex(key, hash);
        }

        hashes[index] = hash;
        keys[index] = keyReferenceStrength.create(key);
        values[index] = producer.produce();
        ++size;
      } else if (keys[index].get() == null) {
        keyDeletionCallback.apply(values[index]);
        hashes[index] = hash;
        keys[index] = keyReferenceStrength.create(key);
        values[index] = producer.produce();
      }

      return values[index];
    }

    @Override
    public int capacity() {
      return capacity;
    }

    @Override
    public int size() {
      return size;
    }

    private boolean rehash() {
      this.size = 0;
      for (int i = 0; i < this.capacity; ++i) {
        if (this.keys[i] == null || this.keys[i].get() == null) continue;

        ++this.size;
      }

      if (3 * this.size < this.capacity) return false;

      Reference<K>[] keys = this.keys;
      V[] values = this.values;
      int[] hashes = this.hashes;
      int capacity = this.capacity;

      this.capacity <<= 1;
      this.keys = newKeys(this.capacity);
      this.values = newValues(this.capacity);
      this.hashes = new int[this.capacity];

      for (int i = 0; i < capacity; ++i) {
        if (keys[i] == null) {
          continue;
        }

        int index = keyIndex(keys[i].get(), hashes[i]);
        this.keys[index] = keys[i];
        this.values[index] = values[i];
        this.hashes[index] = hashes[i];
      }

      return true;
    }

    private int keyIndex(final K key, int hash) {
      hash &= capacity - 1;
      if (keys[hash] == null) return hash;

      K keyValue = keys[hash].get();
      if (keyValue == null || equivalence.test(key, keyValue)) return hash;

      int g = hash(hash) | 1;
      while (true) {
        hash = (hash + g) & (capacity - 1);
        if (keys[hash] == null) return hash;
        keyValue = keys[hash].get();
        if (keyValue == null || equivalence.test(key, keyValue)) return hash;
      }
    }

    private int hash(int hash) {
      hash *= 0xcc9e2d51;
      hash = (hash << 15) | (hash >> 17);
      hash *= 0x1b873593;
      hash = (hash << 13) | (hash >> 19);
      hash ^= hash >> 16;
      hash *= 0x85ebca6b;
      hash ^= hash >> 13;
      hash *= 0xc2b2ae35;
      hash ^= hash >> 16;
      return hash;
    }

    @SuppressWarnings("unchecked")
    private Reference<K>[] newKeys(final int capacity) {
      return (Reference<K>[]) new Reference[capacity];
    }

    @SuppressWarnings("unchecked")
    private V[] newValues(final int capacity) {
      return (V[]) new Object[capacity];
    }

    @Override
    public Map.Iterator<K, V> iterator() {
      return new Iterator();
    }

    private class Iterator implements Map.Iterator<K, V> {
      private int index;

      public Iterator() {
        index = 0;
        advance();
      }

      private void advance() {
        while (hasNext() && keys[index] == null) {
          ++index;
        }
      }

      @Override
      public boolean hasNext() {
        return index < capacity;
      }

      @Override
      public V value() {
        return values[index];
      }

      @Override
      public K key() {
        return keys[index].get();
      }

      @Override
      public void next() {
        ++index;
        advance();
      }
    }
  }
}

package ch.usi.inf.profiler;

public class HashMap<K extends Hashable, V> {
  private static final int INITIAL_CAPACITY = 1 << 10;

  private int capacity;
  private int size;

  private K[] keys;
  private V[] values;
  private int[] hashes;

  public HashMap() {
    this(INITIAL_CAPACITY);
  }

  public HashMap(int initialCapacity) {
    if (initialCapacity == 0 || (initialCapacity & (initialCapacity - 1)) != 0)
      throw new IllegalArgumentException("The initial capacity must be a power of two");

    capacity = initialCapacity;
    size = 0;
    keys = newKeys(capacity);
    values = newValues(capacity);
    hashes = new int[capacity];
  }

  public V getOrPut(K key) {
    int hash = key.hash();
    return null;
  }

  private void rehash() {
    int capacity = this.capacity;
    this.capacity <<= 1;
    K[] keys = newKeys(capacity);
    V[] values = newValues(capacity);
    int[] hashes = new int[capacity];

    for (int i = 0; i < capacity; ++i) {
      if (this.keys[i] == null) continue;

      int index = keyIndex(this.keys[i], this.hashes[i]);
      keys[index] = this.keys[i];
      values[index] = this.values[i];
      hashes[index] = this.hashes[i];
    }

    this.keys = keys;
    this.values = values;
    this.hashes = hashes;
  }

  private int keyIndex(K key, int hash) {
    hash &= capacity - 1;
    if (keys[hash] == null) return hash;

    int g = hash(hash) | 1;
    do hash = (hash + g) & (capacity - 1);
    while (keys[hash] == null);

    return hash;
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
  private K[] newKeys(int capacity) {
    return (K[]) new Object[capacity];
  }

  @SuppressWarnings("unchecked")
  private V[] newValues(int capacity) {
    return (V[]) new Object[capacity];
  }
}

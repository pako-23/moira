package ch.usi.inf.profiler;

public class ArrayMap<V> implements Map<Integer, V> {
  private V map[];
  private int capacity;

  public ArrayMap(final int capacity) {
    map = newMap(capacity);
    this.capacity = capacity;
  }

  @Override
  public int capacity() {
    return capacity;
  }

  @Override
  public int size() {
    return capacity;
  }

  public V getOrPut(Integer key, ValueProducer<V> producer) {
    V value = map[key];

    if (value == null) {
      value = producer.produce();
      map[key] = value;
    }

    return value;
  }

  public Map.Iterator<Integer, V> iterator() {
    return new Iterator();
  }

  @SuppressWarnings("unchecked")
  private V[] newMap(int capacity) {
    return (V[]) new Object[capacity];
  }

  private class Iterator implements Map.Iterator<Integer, V> {
    private int index;

    public Iterator() {
      index = 0;
      advance();
    }

    private void advance() {
      while (hasNext() && map[index] == null) {
        ++index;
      }
    }

    @Override
    public boolean hasNext() {
      return index < capacity;
    }

    @Override
    public V value() {
      return map[index];
    }

    @Override
    public Integer key() {
      return index;
    }

    @Override
    public void next() {
      ++index;
      advance();
    }
  }
}

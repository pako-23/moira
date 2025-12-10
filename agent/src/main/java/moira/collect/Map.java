package moira.collect;

public interface Map<K, V> {
  @FunctionalInterface
  public interface ValueProducer<V> {
    public V produce();
  }

  public interface Iterator<K, V> {
    public boolean hasNext();

    public V value();

    public K key();

    public void next();
  }

  public int capacity();

  public int size();

  public boolean contains(final K key);

  public V get(final K key);

  public V getOrPut(final K key, final ValueProducer<V> producer);

  public Iterator<K, V> iterator();
}

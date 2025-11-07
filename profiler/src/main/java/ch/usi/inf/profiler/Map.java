package ch.usi.inf.profiler;

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

  public boolean contains(K key);

  public V getOrPut(K key, ValueProducer<V> producer);

  public Iterator<K, V> iterator();
}

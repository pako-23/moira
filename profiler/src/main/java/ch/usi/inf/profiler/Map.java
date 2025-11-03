package ch.usi.inf.profiler;

public interface Map<K, V> {
  @FunctionalInterface
  public interface ValueProducer<V> {
    public V produce();
  }

  public int capacity();

  public int size();

  public V getOrPut(K key, ValueProducer<V> producer);
}

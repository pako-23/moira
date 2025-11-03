package ch.usi.inf.profiler;

@FunctionalInterface
public interface HashFunction<K> {
  public int compute(K value);
}

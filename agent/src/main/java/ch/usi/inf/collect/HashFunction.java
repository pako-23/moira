package ch.usi.inf.collect;

@FunctionalInterface
public interface HashFunction<K> {
  public int compute(K value);
}

package ch.usi.inf.moira.collect;

@FunctionalInterface
public interface HashFunction<K> {
  public int compute(K value);
}

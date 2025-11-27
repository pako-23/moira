package ch.usi.inf.collect;

@FunctionalInterface
public interface Equivalence<T> {
  public boolean test(T first, T second);
}

package util;

import java.io.Serializable;

public class Pair<T1, T2> implements Serializable {

  public T1 mFirst;
  public T2 mSecond;

  public Pair(T1 first, T2 second) {
    mFirst = first;
    mSecond = second;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Pair<?, ?> pair = (Pair<?, ?>) o;

    if (!mFirst.equals(pair.mFirst)) {
      return false;
    }
    return mSecond.equals(pair.mSecond);
  }

  @Override
  public int hashCode() {
    int result = mFirst.hashCode();
    result = 31 * result + mSecond.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "" + mFirst + " " + mSecond;
  }
}
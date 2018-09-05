package util;

public class MutableInt {

  private int mValue;

  public MutableInt() {
    this(0);
  }

  public MutableInt(int initialVal) {
    mValue = initialVal;
  }

  public void increment() {
    mValue++;
  }

  public int get() {
    return mValue;
  }

  public void set(int value) {
    mValue = value;
  }
}

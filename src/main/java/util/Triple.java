package util;

import java.io.Serializable;

public class Triple <T1, T2, T3> implements Serializable {
    public T1 mFirst;
    public T2 mSecond;
    public T3 mThird;

    public Triple(T1 mFirst, T2 mSecond, T3 mThird) {
        this.mFirst = mFirst;
        this.mSecond = mSecond;
        this.mThird = mThird;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;

        if (mFirst != null ? !mFirst.equals(triple.mFirst) : triple.mFirst != null) return false;
        if (mSecond != null ? !mSecond.equals(triple.mSecond) : triple.mSecond != null) return false;
        return mThird != null ? mThird.equals(triple.mThird) : triple.mThird == null;
    }

    @Override
    public int hashCode() {
        int result = mFirst != null ? mFirst.hashCode() : 0;
        result = 31 * result + (mSecond != null ? mSecond.hashCode() : 0);
        result = 31 * result + (mThird != null ? mThird.hashCode() : 0);
        return result;
    }
}

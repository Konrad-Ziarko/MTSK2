package fom;

/**
 * Created by konrad on 6/3/17.
 */
public class FomObjectDefinition<T1, T2> {
    private T1 t1;
    private T2 t2;

    public FomObjectDefinition(T1 t1, T2 b) {
        this.t1 = t1;
        this.t2 = t2;
    }

    public T1 getT1() {
        return t1;
    }

    public void setT1(T1 a) {
        this.t1 = t1;
    }

    public T2 getT2() {
        return t2;
    }

    public void setT2(T2 b) {
        this.t2 = t2;
    }

}
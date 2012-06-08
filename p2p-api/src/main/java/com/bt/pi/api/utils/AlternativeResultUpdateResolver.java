package com.bt.pi.api.utils;

import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.entity.PiEntity;

public abstract class AlternativeResultUpdateResolver<T extends Object, P extends PiEntity> implements UpdateResolver<P> {

    private T result;

    /**
     * @return the result
     */
    public T getResult() {
        return result;
    }

    /**
     * @param result
     *            the result to set
     */
    public void setResult(T t) {
        this.result = t;
    }

}

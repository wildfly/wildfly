package org.wildfly.clustering.singleton;

import java.util.concurrent.atomic.AtomicReference;

public class SingletonValueCommand<T> implements SingletonCommand<AtomicReference<T>, T> {
    private static final long serialVersionUID = -2849349352107418635L;

    @Override
    public AtomicReference<T> execute(SingletonContext<T> context) {
        return context.getValueRef();
    }
}

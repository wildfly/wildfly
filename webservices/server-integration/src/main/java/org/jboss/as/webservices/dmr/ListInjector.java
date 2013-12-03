package org.jboss.as.webservices.dmr;

import java.util.List;

import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.RetainingInjector;
import org.jboss.msc.value.Value;

public final class ListInjector<T> extends RetainingInjector<T> implements Injector<T> {
    private final List<T> list;

    public ListInjector(final List<T> list) {
        this.list = list;
    }

    /** {@inheritDoc} */
    public void inject(final T value) throws InjectionException {
        synchronized (list) {
            if (value != null) {
                list.add(value);
            }
            super.inject(value);
        }
    }

    /** {@inheritDoc} */
    public void uninject() {
        synchronized (list) {
            try {
                final Value<T> storedValue = getStoredValue();
                if (storedValue != null) list.remove(storedValue.getValue());
            } finally {
                super.uninject();
            }
        }
    }
}
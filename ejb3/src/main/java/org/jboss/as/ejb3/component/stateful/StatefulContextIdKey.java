package org.jboss.as.ejb3.component.stateful;

import java.io.Serializable;

/**
 * Class that is used as a map key for component instance association
 *
 * @author Stuart Douglas
 */
public final class StatefulContextIdKey implements Serializable {

    public static final StatefulContextIdKey INSTANCE = new StatefulContextIdKey();

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o.getClass().equals(getClass());
    }
}

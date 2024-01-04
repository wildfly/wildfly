/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling;

import java.util.Comparator;

/**
 * @author Paul Ferraro
 */
public class TestComparator<T> implements Comparator<T>, java.io.Serializable {
    private static final long serialVersionUID = 2322453812130991741L;

    @Override
    public int compare(T object1, T object2) {
        return object1.hashCode() - object2.hashCode();
    }
}

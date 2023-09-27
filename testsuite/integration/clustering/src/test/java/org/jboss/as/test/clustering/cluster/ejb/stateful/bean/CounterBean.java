/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.stateful.bean;

import java.io.Serializable;

/**
 * @author Stuart Douglas
 */
public class CounterBean implements Serializable, Counter {
    private static final long serialVersionUID = 5616577826029047421L;

    @Override
    public int getCount() {
        return 10000000;
    }
}

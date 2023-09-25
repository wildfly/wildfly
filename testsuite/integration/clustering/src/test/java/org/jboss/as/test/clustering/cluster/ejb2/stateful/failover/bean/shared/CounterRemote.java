/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb2.stateful.failover.bean.shared;

import jakarta.ejb.EJBObject;

/**
 * @author Ondrej Chaloupka
 */
public interface CounterRemote extends EJBObject {
    CounterResult increment();

    CounterResult decrement();

    CounterResult getCount();
}

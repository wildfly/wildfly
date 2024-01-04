/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.jndi;

/**
 * @author Jaikiran Pai
 */
public interface RemoteCounter {

    int getCount();

    void incrementCount();

    void decrementCount();
}

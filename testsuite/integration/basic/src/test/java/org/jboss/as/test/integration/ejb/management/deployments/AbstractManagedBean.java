/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.management.deployments;

import jakarta.ejb.Asynchronous;
import jakarta.ejb.Remove;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class AbstractManagedBean implements BusinessInterface {
    @Override
    public void doIt() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
        }
    }

    @Remove
    public void remove() {
    }

    @Asynchronous
    public void async(int a, int b) {
    }
}

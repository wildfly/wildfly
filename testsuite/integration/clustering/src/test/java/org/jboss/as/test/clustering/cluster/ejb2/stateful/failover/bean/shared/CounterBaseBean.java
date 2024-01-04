/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb2.stateful.failover.bean.shared;

import java.rmi.RemoteException;

import jakarta.ejb.CreateException;

import org.jboss.as.test.clustering.NodeNameGetter;

/**
 * @author Ondrej Chaloupka
 */
public abstract class CounterBaseBean {
    private int count;

    public void ejbCreate() throws RemoteException, CreateException {
        // Creating method for home interface...
    }

    public CounterResult increment() {
        this.count++;
        return new CounterResult(this.count, getNodeName());
    }

    public CounterResult decrement() {
        this.count--;
        return new CounterResult(this.count, getNodeName());
    }

    public CounterResult getCount() {
        return new CounterResult(this.count, getNodeName());
    }

    private String getNodeName() {
        return NodeNameGetter.getNodeName();
    }
}

package org.jboss.as.test.clustering.extended.ejb2.stateful.remote.failover;

import org.jboss.as.test.clustering.NodeNameGetter;

/**
 * @author Ondrej Chaloupka
 */
public abstract class CounterBaseBean {
    private int count;

    public void ejbCreate() throws java.rmi.RemoteException, javax.ejb.CreateException {
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

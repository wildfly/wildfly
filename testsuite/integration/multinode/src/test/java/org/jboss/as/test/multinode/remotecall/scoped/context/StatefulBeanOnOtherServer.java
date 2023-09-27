/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.remotecall.scoped.context;

import jakarta.ejb.Remote;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.Stateful;

/**
 * @author Jaikiran Pai
 */
@Stateful
@Remote(StatefulRemoteOnOtherServer.class)
@RemoteHome(StatefulRemoteHomeForBeanOnOtherServer.class)
public class StatefulBeanOnOtherServer implements StatefulRemoteOnOtherServer {
    private int count;

    @Override
    public int getCount() {
        return this.count;
    }

    @Override
    public int incrementCount() {
        this.count++;
        return this.count;
    }

    public void ejbCreate() {

    }

    public void ejbCreateDifferentWay() {

    }

    public void ejbCreateYetAnotherWay(final int initialCount) {
        this.count = initialCount;
    }
}

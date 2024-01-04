/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.remotecall.scoped.context;

import jakarta.ejb.CreateException;
import jakarta.ejb.EJBHome;

/**
 * @author Jaikiran Pai
 */
public interface StatefulRemoteHomeForBeanOnOtherServer extends EJBHome {

    StatefulRemoteOnOtherServer create() throws CreateException;

    StatefulRemoteOnOtherServer createDifferentWay() throws CreateException;

    StatefulRemoteOnOtherServer createYetAnotherWay(int initialCount) throws CreateException;
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.remotecall.scoped.context;

/**
 * @author Jaikiran Pai
 */
public interface StatefulRemoteOnOtherServer {

    int getCount();

    int incrementCount();
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.remotecall.scoped.context;

import java.util.concurrent.CountDownLatch;

/**
 * @author Jaikiran Pai
 */
public interface LocalServerStatefulRemote {

    int getCountByInvokingOnRemoteServerBean();

    int incrementCountByInvokingOnRemoteServerBean();

    String getEchoByInvokingOnRemoteServerBean(final String msg);

    void registerPassivationNotificationLatch(final CountDownLatch latch);

    boolean wasPostActivateInvoked();

    StatefulRemoteOnOtherServer getSFSBCreatedWithScopedEJBClientContext();

    StatelessRemoteOnOtherServer getSLSBCreatedWithScopedEJBClientContext();
}

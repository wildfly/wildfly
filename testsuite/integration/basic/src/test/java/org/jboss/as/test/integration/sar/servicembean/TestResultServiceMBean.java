/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.servicembean;

/**
 * MBean interface for {@link TestResultService}.
 *
 * @author Eduardo Martins
 */
public interface TestResultServiceMBean {

    boolean isCreateServiceInvoked();

    boolean isStartServiceInvoked();

    boolean isStopServiceInvoked();

    boolean isDestroyServiceInvoked();

    void setCreateServiceInvoked(boolean createServiceInvoked);

    void setDestroyServiceInvoked(boolean destroyServiceInvoked);

    void setStartServiceInvoked(boolean startServiceInvoked);

    void setStopServiceInvoked(boolean stopServiceInvoked);

    boolean isStartingNotificationReceived();

    boolean isStartedNotificationReceived();

    boolean isStoppingNotificationReceived();

    boolean isStoppedNotificationReceived();
}

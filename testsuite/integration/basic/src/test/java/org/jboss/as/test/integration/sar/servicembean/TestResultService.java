/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.servicembean;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.NotificationListener;

import org.jboss.logging.Logger;
import org.jboss.system.ServiceMBean;

/**
 * An MBean that collects results of life-cycle methods invocations of {@link TestServiceMBean}.
 *
 * @author Eduardo Martins
 */
public class TestResultService implements TestResultServiceMBean, ServiceMBean, NotificationListener {

    private static Logger logger = Logger.getLogger(TestResultService.class.getName());

    private boolean createServiceInvoked;
    private boolean startServiceInvoked;
    private boolean stopServiceInvoked;
    private boolean destroyServiceInvoked;
    private boolean startingNotificationReceived;
    private boolean startedNotificationReceived;
    private boolean stoppingNotificationReceived;
    private boolean stoppedNotificationReceived;


    @Override
    public boolean isCreateServiceInvoked() {
        return createServiceInvoked;
    }

    @Override
    public boolean isDestroyServiceInvoked() {
        return destroyServiceInvoked;
    }

    @Override
    public boolean isStartServiceInvoked() {
        return startServiceInvoked;
    }

    @Override
    public boolean isStopServiceInvoked() {
        return stopServiceInvoked;
    }

    public void setCreateServiceInvoked(boolean createServiceInvoked) {
        this.createServiceInvoked = createServiceInvoked;
    }

    public void setDestroyServiceInvoked(boolean destroyServiceInvoked) {
        this.destroyServiceInvoked = destroyServiceInvoked;
    }

    public void setStartServiceInvoked(boolean startServiceInvoked) {
        this.startServiceInvoked = startServiceInvoked;
    }

    public void setStopServiceInvoked(boolean stopServiceInvoked) {
        this.stopServiceInvoked = stopServiceInvoked;
    }

    @Override
    public boolean isStartingNotificationReceived() {
        return startingNotificationReceived;
    }

    @Override
    public boolean isStartedNotificationReceived() {
        return startedNotificationReceived;
    }

    @Override
    public boolean isStoppingNotificationReceived() {
        return stoppingNotificationReceived;
    }

    @Override
    public boolean isStoppedNotificationReceived() {
        return stoppedNotificationReceived;
    }

    @Override
    public void create() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void start() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub

    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getState() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getStateString() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void jbossInternalLifecycle(String method) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
        if (notification instanceof AttributeChangeNotification) {
            AttributeChangeNotification attributeChangeNotification
                    = (AttributeChangeNotification) notification;
            int oldValue = (Integer) attributeChangeNotification.getOldValue();
            int newValue = (Integer) attributeChangeNotification.getNewValue();
            logger.trace("Attribute change notification: " + oldValue + "->" + newValue);
            if (oldValue == STOPPED && newValue == STARTING) { startingNotificationReceived = true; } else if (oldValue == STARTING && newValue == STARTED) {
                startedNotificationReceived = true;
            } else if (oldValue == STARTED && newValue == STOPPING) {
                stoppingNotificationReceived = true;
            } else if (oldValue == STOPPING && newValue == STOPPED) {
                stoppedNotificationReceived = true;
            }
        }
    }

}

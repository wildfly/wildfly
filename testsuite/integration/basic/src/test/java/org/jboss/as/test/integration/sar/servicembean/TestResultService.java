/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

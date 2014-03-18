/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.notification;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.notification.NotificationFilter.ALL;
import static org.jboss.as.controller.notification.NotificationSupport.ANY_ADDRESS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.test.AbstractControllerTestBase;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class OperationWithNotificationTestCase extends AbstractControllerTestBase {

    private static final String OPERATION_THAT_EMITS_A_NOTIFICATION = "operation-that-emits-a-notification";
    private static final String MY_TYPE = "my-notification-type";

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        registration.registerOperationHandler(new SimpleOperationDefinitionBuilder(OPERATION_THAT_EMITS_A_NOTIFICATION, new NonResolvingResourceDescriptionResolver())
                .setPrivateEntry()
                .build(),
                new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        Notification notification = new Notification(MY_TYPE, operation.get(OP_ADDR), "notification message");
                        context.emit(notification);
                        context.stepCompleted();
                    }
                }
        );
    }

    @Test
    public void testSendNotification() throws Exception {
        final CountDownLatch notificationEmittedLatch = new CountDownLatch(1);
        getController().getNotificationSupport().registerNotificationHandler(ANY_ADDRESS, new NotificationHandler() {
                    @Override
                    public void handleNotification(Notification notification) {
                        notificationEmittedLatch.countDown();
                    }
                }, new NotificationFilter() {

                    @Override
                    public boolean isNotificationEnabled(Notification notification) {
                        return MY_TYPE.equals(notification.getType());
                    }
                }
        );

        executeForResult(createOperation(OPERATION_THAT_EMITS_A_NOTIFICATION));
        assertTrue("the notification handler did not receive the notification", notificationEmittedLatch.await(1, SECONDS));
    }

    @Test
    public void testSendNotificationWithFilter() throws Exception {

        final CountDownLatch notificationEmittedLatch = new CountDownLatch(1);
        getController().getNotificationSupport().registerNotificationHandler(ANY_ADDRESS, new NotificationHandler() {
                    @Override
                    public void handleNotification(Notification notification) {
                        notificationEmittedLatch.countDown();
                    }
                }, new NotificationFilter() {

                    @Override
                    public boolean isNotificationEnabled(Notification notification) {
                        return false;
                    }
                });

        executeForResult(createOperation(OPERATION_THAT_EMITS_A_NOTIFICATION));
        assertFalse("the notification handler must not received the filtered out notification", notificationEmittedLatch.await(250, MILLISECONDS));
    }

    @Test
    public void testSendNotificationToFailingHandler() throws Exception {
        final CountDownLatch notificationEmittedLatch = new CountDownLatch(1);
        getController().getNotificationSupport().registerNotificationHandler(ANY_ADDRESS, new NotificationHandler() {
            @Override
            public void handleNotification(Notification notification) {
                // the handler receives the notification
                notificationEmittedLatch.countDown();
                // but fails to process it
                throw new IllegalStateException("somehow, the handler throws an exception");
            }
        }, ALL);

        // having a failing notification handler has no incidence on the operation that triggered the notification emission
        executeForResult(createOperation(OPERATION_THAT_EMITS_A_NOTIFICATION));
        assertTrue("the notification handler did receive the notification", notificationEmittedLatch.await(1, SECONDS));
    }

    @Test
    public void testSendNotificationToFailingFilter() throws Exception {
        final CountDownLatch notificationEmittedLatch = new CountDownLatch(1);
        getController().getNotificationSupport().registerNotificationHandler(ANY_ADDRESS, new NotificationHandler() {
                    @Override
                    public void handleNotification(Notification notification) {
                        notificationEmittedLatch.countDown();
                    }
                }, new NotificationFilter() {
                    @Override
                    public boolean isNotificationEnabled(Notification notification) {
                        throw new IllegalStateException("somehow, the filter throws an exception");
                    }
                });
        // having a failing notification filter has no incidence on the operation that triggered the notification emission
        executeForResult(createOperation(OPERATION_THAT_EMITS_A_NOTIFICATION));
        assertFalse("the notification handler did receive the notification", notificationEmittedLatch.await(256, MILLISECONDS));
    }

    @Test
    public void testSendNotificationAfterUnregisteringHandler() throws Exception {
        final CountDownLatch notificationEmittedLatch = new CountDownLatch(1);

        NotificationHandler handler = new NotificationHandler() {
            @Override
            public void handleNotification(Notification notification) {
                notificationEmittedLatch.countDown();
            }
        };
        // register the handler...
        getController().getNotificationSupport().registerNotificationHandler(ANY_ADDRESS, handler, ALL);
        // ... and unregister it
        getController().getNotificationSupport().unregisterNotificationHandler(ANY_ADDRESS, handler, ALL);
        executeForResult(createOperation(OPERATION_THAT_EMITS_A_NOTIFICATION));
        assertFalse("the unregistered notification handler did not receive the notification", notificationEmittedLatch.await(256, MILLISECONDS));
    }
}

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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.notification.NotificationFilter.ALL;
import static org.jboss.as.controller.notification.NotificationRegistry.ANY_ADDRESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.test.AbstractControllerTestBase;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *
 * Test handling notifications and filtering them.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class OperationWithNotificationTestCase extends AbstractControllerTestBase {

    private static final String MY_OPERATION = "my-operation";
    private static final String MY_NOTIFICATION_TYPE = "my-notification-type";

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        registration.registerOperationHandler(new SimpleOperationDefinitionBuilder(MY_OPERATION, new NonResolvingResourceDescriptionResolver())
                        .setPrivateEntry()
                        .build(),
                new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        Notification notification = new Notification(MY_NOTIFICATION_TYPE, PathAddress.pathAddress(operation.get(OP_ADDR)), "notification message");
                        context.emit(notification);
                        context.stepCompleted();
                    }
                }
        );
    }

    @Test
    public void testSendNotification() throws Exception {
        ListBackedNotificationHandler handler = new ListBackedNotificationHandler();
        getController().getNotificationRegistry().registerNotificationHandler(ANY_ADDRESS, handler, new NotificationFilter() {

                    @Override
                    public boolean isNotificationEnabled(Notification notification) {
                        return MY_NOTIFICATION_TYPE.equals(notification.getType());
                    }
                }
        );

        executeForResult(createOperation(MY_OPERATION));
        assertEquals("the notification handler did not receive the notification", 1, handler.getNotifications().size());

        getController().getNotificationRegistry().unregisterNotificationHandler(ANY_ADDRESS, handler, ALL);
    }

    @Test
    public void testSendNotificationWithFilter() throws Exception {
        ListBackedNotificationHandler handler = new ListBackedNotificationHandler();
        getController().getNotificationRegistry().registerNotificationHandler(ANY_ADDRESS, handler, new NotificationFilter() {

            @Override
            public boolean isNotificationEnabled(Notification notification) {
                return false;
            }
        });

        executeForResult(createOperation(MY_OPERATION));
        assertTrue("the notification handler must not receive the filtered out notification", handler.getNotifications().isEmpty());

        getController().getNotificationRegistry().unregisterNotificationHandler(ANY_ADDRESS, handler, ALL);
    }

    @Test
    public void testSendNotificationToFailingHandler() throws Exception {
        final AtomicBoolean gotNotification = new AtomicBoolean(false);
        NotificationHandler handler = new NotificationHandler() {
            @Override
            public void handleNotification(Notification notification) {
                // the handler receives the notification
                gotNotification.set(true);
                // but fails to process it
                throw new IllegalStateException("somehow, the handler throws an exception");
            }
        };

        getController().getNotificationRegistry().registerNotificationHandler(ANY_ADDRESS, handler, ALL);

        // having a failing notification handler has no incidence on the operation that triggered the notification emission
        executeForResult(createOperation(MY_OPERATION));
        assertTrue("the notification handler did receive the notification", gotNotification.get());

        getController().getNotificationRegistry().unregisterNotificationHandler(ANY_ADDRESS, handler, ALL);
    }

    @Test
    public void testSendNotificationWithFailingFilter() throws Exception {
        ListBackedNotificationHandler handler = new ListBackedNotificationHandler();
        getController().getNotificationRegistry().registerNotificationHandler(ANY_ADDRESS, handler, new NotificationFilter() {
            @Override
            public boolean isNotificationEnabled(Notification notification) {
                throw new IllegalStateException("somehow, the filter throws an exception");
            }
        });
        // having a failing notification filter has no incidence on the operation that triggered the notification emission
        executeForResult(createOperation(MY_OPERATION));
        // but the handler will not be notified
        assertTrue("the notification handler did receive the notification", handler.getNotifications().isEmpty());

        getController().getNotificationRegistry().unregisterNotificationHandler(ANY_ADDRESS, handler, ALL);
    }

    @Test
    public void testSendNotificationAfterUnregisteringHandler() throws Exception {
        ListBackedNotificationHandler handler = new ListBackedNotificationHandler();
        // register the handler...
        getController().getNotificationRegistry().registerNotificationHandler(ANY_ADDRESS, handler, ALL);
        // ... and unregister it
        getController().getNotificationRegistry().unregisterNotificationHandler(ANY_ADDRESS, handler, ALL);
        executeForResult(createOperation(MY_OPERATION));
        assertTrue("the unregistered notification handler did not receive the notification", handler.getNotifications().isEmpty());
    }
}

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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.notification.NotificationSupport.ANY_ADDRESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.jboss.as.controller.CompositeOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.test.AbstractControllerTestBase;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class NotificationCompositeOperationTestCase extends AbstractControllerTestBase {

    private static final String OPERATION_THAT_EMITS_A_NOTIFICATION = "operation-that-emits-a-notification";
    private static final String MY_TYPE = "my-notification-type";

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        registration.registerOperationHandler(CompositeOperationHandler.DEFINITION, CompositeOperationHandler.INSTANCE);

        registration.registerOperationHandler(new SimpleOperationDefinitionBuilder(OPERATION_THAT_EMITS_A_NOTIFICATION, new NonResolvingResourceDescriptionResolver())
                .setPrivateEntry()
                .build(),
                new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        Notification notification = new Notification(MY_TYPE, operation.get(OP_ADDR), operation.get("param").asString());
                        context.emit(notification);
                        context.stepCompleted();
                    }
                }
        );
    }

    @Test
    public void testCompositeOperationThatEmitsNotifications() throws Exception {

        final CountDownLatch notificationEmittedLatch = new CountDownLatch(2);
        final List<Notification> receivedNotifications = new ArrayList<>();
        getController().getNotificationSupport().registerNotificationHandler(ANY_ADDRESS, new NotificationHandler() {
                    @Override
                    public void handleNotification(Notification notification) {
                        receivedNotifications.add(notification);
                        notificationEmittedLatch.countDown();
                    }
                }, new NotificationFilter() {

                    @Override
                    public boolean isNotificationEnabled(Notification notification) {
                        return MY_TYPE.equals(notification.getType());
                    }
                }
        );

        ModelNode operation = new ModelNode();
        operation.get(OP).set("composite");
        operation.get(OP_ADDR).setEmptyList();
        ModelNode op1 = createOperation(OPERATION_THAT_EMITS_A_NOTIFICATION);
        op1.get("param").set("param1");
        ModelNode op2 = createOperation(OPERATION_THAT_EMITS_A_NOTIFICATION);
        op2.get("param").set("param2");
        operation.get(ModelDescriptionConstants.STEPS).add(op1);
        operation.get(ModelDescriptionConstants.STEPS).add(op2);
        ModelNode result = executeForResult(operation);
        System.out.println("operation = " + operation);
        System.out.println("result = " + result);

        // the notifications are received in the order they were emitted
        assertTrue("the notification were not emitted", notificationEmittedLatch.await(1, SECONDS));
        assertEquals(receivedNotifications.toString(), 2, receivedNotifications.size());
        System.out.println("receivedNotifications = " + receivedNotifications);
        assertEquals("param1", receivedNotifications.get(0).getMessage());
        assertEquals("param2", receivedNotifications.get(1).getMessage());
    }
}

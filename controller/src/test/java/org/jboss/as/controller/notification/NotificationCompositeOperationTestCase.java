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
import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.notification.NotificationFilter.ALL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.CompositeOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.test.AbstractControllerTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class NotificationCompositeOperationTestCase extends AbstractControllerTestBase {


    public static final String OPERATION_THAT_EMITS_A_NOTIFICATION = "operation-that-emits-a-notification";
    public static final String OPERATION_THAT_REGISTERS_A_NOTIFICATION_HANDLER = "operation-that-registers-a-notification-handler";
    public static final String OPERATION_THAT_UNREGISTERS_A_NOTIFICATION_HANDLER = "operation-that-unregisters-a-notification-handler";

    private DelegatingNotificationHandler delegatingNotificationHandler = new DelegatingNotificationHandler();

    public static final AtomicReference<CountDownLatch> notificationEmittedLatch = new AtomicReference<>();
    public static final AtomicReference<CountDownLatch> notificationHandlerRegisteredLatch = new AtomicReference<>();
    public static final AtomicReference<CountDownLatch> notificationHandlerUnregisteredLatch = new AtomicReference<>();
    public static final String MY_TYPE = "my-notification-type";

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        notificationHandlerRegisteredLatch.set(new CountDownLatch(1));
        notificationEmittedLatch.set(new CountDownLatch(2));
        notificationHandlerUnregisteredLatch.set(new CountDownLatch(1));

        registration.registerOperationHandler(CompositeOperationHandler.DEFINITION, CompositeOperationHandler.INSTANCE);

        registration.registerOperationHandler(new SimpleOperationDefinitionBuilder(OPERATION_THAT_EMITS_A_NOTIFICATION, new NonResolvingResourceDescriptionResolver())
                .setPrivateEntry()
                .build(),
                new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        context.completeStep(new OperationContext.ResultHandler() {
                            @Override
                            public void handleResult(OperationContext.ResultAction resultAction, OperationContext context, ModelNode operation) {
                                Notification notification = new Notification(MY_TYPE, pathAddress(operation.get(OP_ADDR)), operation.get("param").asString());
                                context.getNotificationSupport().emit(notification);
                            }
                        });
                    }
                }
        );
        registration.registerOperationHandler(new SimpleOperationDefinitionBuilder(OPERATION_THAT_REGISTERS_A_NOTIFICATION_HANDLER, new NonResolvingResourceDescriptionResolver())
                .setPrivateEntry()
                .build(),
                new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        PathAddress source = pathAddress(operation.get(OP_ADDR));
                        context.getNotificationSupport().registerNotificationHandler(source, delegatingNotificationHandler, delegatingNotificationHandler);
                        context.stepCompleted();
                        notificationHandlerRegisteredLatch.get().countDown();
                    }
                }
        );
        registration.registerOperationHandler(new SimpleOperationDefinitionBuilder(OPERATION_THAT_UNREGISTERS_A_NOTIFICATION_HANDLER, new NonResolvingResourceDescriptionResolver())
                .setPrivateEntry()
                .build(),
                new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        PathAddress source = pathAddress(operation.get(OP_ADDR));
                        context.getNotificationSupport().unregisterNotificationHandler(source, delegatingNotificationHandler, delegatingNotificationHandler);
                        context.stepCompleted();
                        notificationHandlerUnregisteredLatch.get().countDown();
                    }
                }
        );
    }

    @Ignore
    @Test
    public void testCompositeOperationThatEmitsNotifications() throws Exception {
        final List<Notification> receivedNotifications = new ArrayList<Notification>();

        delegatingNotificationHandler.setDelegatingHandler(new NotificationHandler() {
            @Override
            public void handleNotification(Notification notification) {
                receivedNotifications.add(notification);
                if (MY_TYPE.equals(notification.getType())) {
                    notificationEmittedLatch.get().countDown();
                }
            }
        });
        delegatingNotificationHandler.setDelegatingFilter(ALL);
        executeForResult(createOperation(OPERATION_THAT_REGISTERS_A_NOTIFICATION_HANDLER));
        assertTrue("the notification handler was not registered", notificationHandlerRegisteredLatch.get().await(1, SECONDS));


        ModelNode operation = new ModelNode();
        operation.get(OP).set("composite");
        operation.get(OP_ADDR).setEmptyList();
        ModelNode op1 = createOperation(OPERATION_THAT_EMITS_A_NOTIFICATION);
        op1.get("param").set("param1");
        ModelNode op2 = createOperation(OPERATION_THAT_EMITS_A_NOTIFICATION);
        op2.get("param").set("param2");
        operation.get(ModelDescriptionConstants.STEPS).add(op1);
        operation.get(ModelDescriptionConstants.STEPS).add(op2);
        executeForResult(operation);

        // expect the notifications in the order they were emitted by the composite operation steps
        assertTrue("the notification were not emitted", notificationEmittedLatch.get().await(1, SECONDS));
        assertEquals(2, receivedNotifications.size());
        assertEquals("param1", receivedNotifications.get(0).getMessage());
        assertEquals("param2", receivedNotifications.get(1).getMessage());

        executeForResult(createOperation(OPERATION_THAT_UNREGISTERS_A_NOTIFICATION_HANDLER));
        assertTrue("the notification handler was not unregistered", notificationHandlerUnregisteredLatch.get().await(1, SECONDS));
    }
}

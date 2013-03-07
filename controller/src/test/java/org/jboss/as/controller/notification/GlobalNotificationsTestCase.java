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
import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_ADDED_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_REMOVED_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.operations.global.GlobalNotifications.NEW_VALUE;
import static org.jboss.as.controller.operations.global.GlobalNotifications.OLD_VALUE;
import static org.jboss.dmr.ModelType.INT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.global.GlobalNotifications;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.test.AbstractControllerTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class GlobalNotificationsTestCase extends AbstractControllerTestBase {

    public static final String OPERATION_THAT_REGISTERS_A_NOTIFICATION_HANDLER = "operation-that-registers-a-notification-handler";

    public static final AtomicReference<CountDownLatch> notificationEmittedLatch = new AtomicReference<>();
    public static final AtomicReference<Notification> notification = new AtomicReference<>();
    public static final AtomicReference<CountDownLatch> notificationHandlerRegisteredLatch = new AtomicReference<>();
    public static final AtomicReference<CountDownLatch> notificationHandlerUnregisteredLatch = new AtomicReference<>();
    public static final AtomicReference<NotificationHandler> notificationHandler = new AtomicReference<>();
    public static final AtomicReference<NotificationFilter> notificationFilter = new AtomicReference<>();

    public static final SimpleAttributeDefinition RESOURCE_ATTRIBUTE = create("attr", INT)
            .setStorageRuntime()
            .setDefaultValue(new ModelNode(12345))
            .build();

    @Override
    protected void initModel(Resource rootResource, final ManagementResourceRegistration rootRegistration) {
        GlobalOperationHandlers.registerGlobalOperations(rootRegistration, processType);
        NotificationService.installNotificationService(getContainer().subTarget());

        notificationEmittedLatch.set(new CountDownLatch(1));
        notificationHandlerRegisteredLatch.set(new CountDownLatch(1));
        notificationHandlerUnregisteredLatch.set(new CountDownLatch(1));

        notificationHandler.set(new NotificationHandler() {
            @Override
            public void handleNotification(Notification notif) {
                System.out.println("notification = " + notif);
                notification.set(notif);
                notificationEmittedLatch.get().countDown();
            }
        });
        notificationFilter.set(NotificationFilter.ALL);


        System.out.println("GlobalNotificationsTestCase.initModel");
        rootRegistration.registerOperationHandler(new SimpleOperationDefinitionBuilder(OPERATION_THAT_REGISTERS_A_NOTIFICATION_HANDLER, new NonResolvingResourceDescriptionResolver())
                    .setPrivateEntry()
                    .build(),
                new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        ServiceController<?> notificationService = context.getServiceRegistry(false).getService(NotificationService.SERVICE_NAME);
                        NotificationSupport notificationSupport = NotificationSupport.class.cast(notificationService.getValue());
                        ModelNode resource = new ModelNode();
                        resource.add("profile", "*");
                        notificationSupport.registerNotificationHandler(pathAddress(resource), notificationHandler.get(), notificationFilter.get());
                        notificationHandlerRegisteredLatch.get().countDown();
                        context.stepCompleted();
                    }
                }
        );

        ResourceDefinition profileDef = ResourceBuilder.Factory.create(PathElement.pathElement("profile", "*"),
                new NonResolvingResourceDescriptionResolver())
                .setAddOperation(new AbstractAddStepHandler() {
                    @Override
                    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
                        RESOURCE_ATTRIBUTE.validateAndSet(operation, model);
                        System.out.println("model = [" + model + "]");
                    }
                })
                .setRemoveOperation(new AbstractRemoveStepHandler() {
                    // no-op
                })
                .addReadWriteAttributes(null, new AbstractWriteAttributeHandler<Integer>() {
                    @Override
                    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Integer> handbackHolder) throws OperationFailedException {
                        System.out.println("context = " + context);
                        context.stepCompleted();
                        return true;
                    }

                    @Override
                    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Integer handback) throws OperationFailedException {
                    }
                }, RESOURCE_ATTRIBUTE)
                .build();
        rootRegistration.registerSubModel(profileDef);
    }

    @Test
    public void testRESOURCE_ADDED_NOTIFICATION() throws Exception {
        notificationFilter.set(new NotificationFilter() {
            @Override
            public boolean isNotificationEnabled(Notification notification) {
                return RESOURCE_ADDED_NOTIFICATION.equals(notification.getType()) &&
                        pathElement("profile", "myprofile").equals(notification.getResource().getLastElement());
            }
        });

        executeForResult(createOperation(OPERATION_THAT_REGISTERS_A_NOTIFICATION_HANDLER));
        assertTrue("the notification handler was not registered", notificationHandlerRegisteredLatch.get().await(1, SECONDS));

        ModelNode add = createOperation(ADD, "profile", "myprofile");
        executeForResult(add);
        assertTrue("the notification handler did not receive the " + RESOURCE_ADDED_NOTIFICATION, notificationEmittedLatch.get().await(1, SECONDS));
    }

    @Test
    public void testRESOURCE_REMOVED_NOTIFICATION() throws Exception {
        notificationFilter.set(new NotificationFilter() {
            @Override
            public boolean isNotificationEnabled(Notification notification) {
                return RESOURCE_REMOVED_NOTIFICATION.equals(notification.getType()) &&
                        pathElement("profile", "myprofile").equals(notification.getResource().getLastElement());
            }
        });

        executeForResult(createOperation(OPERATION_THAT_REGISTERS_A_NOTIFICATION_HANDLER));
        assertTrue("the notification handler was not registered", notificationHandlerRegisteredLatch.get().await(1, SECONDS));

        executeForResult(createOperation(ADD, "profile", "myprofile"));

        executeForResult(createOperation(REMOVE, "profile", "myprofile"));
        assertTrue("the notification handler did not receive the " + RESOURCE_REMOVED_NOTIFICATION, notificationEmittedLatch.get().await(1, SECONDS));
    }

    @Test
    public void testATTRIBUTE_VALUE_WRITTEN_NOTIFICATION() throws Exception {
        notificationFilter.set(new NotificationFilter() {
            @Override
            public boolean isNotificationEnabled(Notification notification) {
                return ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION.equals(notification.getType());
            }
        });


        executeForResult(createOperation(ADD, "profile", "myprofile"));

        executeForResult(createOperation(OPERATION_THAT_REGISTERS_A_NOTIFICATION_HANDLER));
        assertTrue("the notification handler was not registered", notificationHandlerRegisteredLatch.get().await(1, SECONDS));

        ModelNode readAttribute = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "myprofile");
        readAttribute.get(NAME).set(RESOURCE_ATTRIBUTE.getName());
        ModelNode result = executeForResult(readAttribute);
        // read-attribute returns the default value
        assertEquals(RESOURCE_ATTRIBUTE.getDefaultValue().asInt(), result.asInt());

        System.out.println("result = " + result);

        ModelNode writeAttribute = createOperation(WRITE_ATTRIBUTE_OPERATION, "profile", "myprofile");
        writeAttribute.get(NAME).set(RESOURCE_ATTRIBUTE.getName());
        writeAttribute.get(VALUE).set(56789);
        executeForResult(writeAttribute);
        assertTrue("the notification handler did not receive the " + ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION, notificationEmittedLatch.get().await(1, SECONDS));
        System.out.println("notification = " + notification.get());
        assertEquals(RESOURCE_ATTRIBUTE.getName(), notification.get().getData().require(NAME).asString());
        // the value was not defined: the notification does not return the default value but undefined instead.
        assertFalse(notification.get().getData().require(OLD_VALUE).isDefined());
        assertEquals(56789, notification.get().getData().require(NEW_VALUE).asInt());
    }
}

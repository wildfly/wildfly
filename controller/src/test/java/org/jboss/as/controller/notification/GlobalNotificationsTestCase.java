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
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_ADDED_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_REMOVED_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.operations.global.GlobalNotifications.NEW_VALUE;
import static org.jboss.as.controller.operations.global.GlobalNotifications.OLD_VALUE;
import static org.jboss.dmr.ModelType.LONG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.test.AbstractControllerTestBase;
import org.jboss.dmr.ModelNode;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class GlobalNotificationsTestCase extends AbstractControllerTestBase {

    public static final SimpleAttributeDefinition RESOURCE_ATTRIBUTE = create("attr", LONG)
            .setDefaultValue(new ModelNode(12345))
            .build();

    private static final PathAddress resourceAddressPattern = pathAddress(pathElement("profile", "*"));
    final PathAddress resourceAddress = pathAddress(pathElement("profile", "myprofile"));

    @Override
    protected void initModel(Resource rootResource, final ManagementResourceRegistration rootRegistration) {
        // register the global operations to be able to call :read-attribute and :write-attribute
        GlobalOperationHandlers.registerGlobalOperations(rootRegistration, processType);

        ResourceDefinition profileDefinition = createDummyProfileResourceDefinition();
        rootRegistration.registerSubModel(profileDefinition);
    }

    private static ResourceDefinition createDummyProfileResourceDefinition() {
        return ResourceBuilder.Factory.create(resourceAddressPattern.getElement(0),
                new NonResolvingResourceDescriptionResolver())
                .setAddOperation(new AbstractAddStepHandler() {
                    @Override
                    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
                        RESOURCE_ATTRIBUTE.validateAndSet(operation, model);
                    }
                })
                .setRemoveOperation(new AbstractRemoveStepHandler() {
                    // no-op
                })
                .addReadWriteAttributes(null, new AbstractWriteAttributeHandler<Long>(RESOURCE_ATTRIBUTE) {
                    @Override
                    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Long> handbackHolder) throws OperationFailedException {
                        return false;

                    }

                    @Override
                    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Long handback) throws OperationFailedException {
                    }
                }, RESOURCE_ATTRIBUTE)
                .build();
    }

    @Test
    public void test_RESOURCE_ADDED_NOTIFICATION() throws Exception {
        final CountDownLatch notificationEmittedLatch = new CountDownLatch(1);
        getController().getNotificationSupport().registerNotificationHandler(resourceAddressPattern, new NotificationHandler() {
                    @Override
                    public void handleNotification(Notification notification) {
                        notificationEmittedLatch.countDown();
                    }
                }, new NotificationFilter() {

                    @Override
                    public boolean isNotificationEnabled(Notification notification) {
                        return RESOURCE_ADDED_NOTIFICATION.equals(notification.getType()) &&
                                resourceAddress.equals(pathAddress(notification.getResource()));
                    }
                });

        executeForResult(createOperation(ADD, resourceAddress));
        assertTrue("the notification handler did not receive the " + RESOURCE_ADDED_NOTIFICATION, notificationEmittedLatch.await(1, SECONDS));
    }

    @Test
    public void test_RESOURCE_REMOVED_NOTIFICATION() throws Exception {
        executeForResult(createOperation(ADD, resourceAddress));

        final CountDownLatch notificationEmittedLatch = new CountDownLatch(1);
        getController().getNotificationSupport().registerNotificationHandler(resourceAddressPattern, new NotificationHandler() {
                    @Override
                    public void handleNotification(Notification notification) {
                        notificationEmittedLatch.countDown();
                    }
                }, new NotificationFilter() {

                    @Override
                    public boolean isNotificationEnabled(Notification notification) {
                        return RESOURCE_REMOVED_NOTIFICATION.equals(notification.getType()) &&
                                resourceAddress.equals(pathAddress(notification.getResource()));
                    }
                });

        executeForResult(createOperation(REMOVE, resourceAddress));
        assertTrue("the notification handler did not receive the " + RESOURCE_REMOVED_NOTIFICATION, notificationEmittedLatch.await(1, SECONDS));
    }

    @Test
    public void test_ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION() throws Exception {
        executeForResult(createOperation(ADD, resourceAddress));

        ModelNode readAttribute = createOperation(READ_ATTRIBUTE_OPERATION, resourceAddress);
        readAttribute.get(NAME).set(RESOURCE_ATTRIBUTE.getName());
        ModelNode result = executeForResult(readAttribute);
        // read-attribute returns the default value
        assertEquals(RESOURCE_ATTRIBUTE.getDefaultValue().asLong(), result.asLong());

        final CountDownLatch notificationEmittedLatch = new CountDownLatch(1);
        final AtomicReference<Notification> notification = new AtomicReference<>();
        getController().getNotificationSupport().registerNotificationHandler(resourceAddressPattern, new NotificationHandler() {
                    @Override
                    public void handleNotification(Notification notif) {
                        notification.set(notif);
                        notificationEmittedLatch.countDown();
                    }
                }, new NotificationFilter() {

                    @Override
                    public boolean isNotificationEnabled(Notification notification) {
                        return ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION.equals(notification.getType()) &&
                                resourceAddress.equals(pathAddress(notification.getResource()));
                    }
                });

        long newValue = System.currentTimeMillis();
        ModelNode writeAttribute = createOperation(WRITE_ATTRIBUTE_OPERATION, resourceAddress);
        writeAttribute.get(NAME).set(RESOURCE_ATTRIBUTE.getName());
        writeAttribute.get(VALUE).set(newValue);
        executeForResult(writeAttribute);
        assertTrue("the notification handler did not receive the " + ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION, notificationEmittedLatch.await(1, SECONDS));
        assertEquals(RESOURCE_ATTRIBUTE.getName(), notification.get().getData().require(NAME).asString());
        // the value was not defined: the notification does not return the default value but undefined instead.
        System.out.println("notification = " + notification.get());
        assertFalse(notification.get().getData().require(OLD_VALUE).isDefined());
        assertEquals(newValue, notification.get().getData().require(NEW_VALUE).asLong());
    }

    @Test
    public void test_ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION_NotSentWhenWriteAttributeOperationFails() throws Exception {

        executeForResult(createOperation(ADD, resourceAddress));

        final CountDownLatch notificationEmittedLatch = new CountDownLatch(1);
        getController().getNotificationSupport().registerNotificationHandler(resourceAddressPattern, new NotificationHandler() {
                    @Override
                    public void handleNotification(Notification notif) {
                        notificationEmittedLatch.countDown();
                    }
                }, new NotificationFilter() {

                    @Override
                    public boolean isNotificationEnabled(Notification notification) {
                        return ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION.equals(notification.getType()) &&
                                resourceAddress.equals(pathAddress(notification.getResource()));
                    }
                });

        String incorrectValue = UUID.randomUUID().toString();
        ModelNode writeAttribute = createOperation(WRITE_ATTRIBUTE_OPERATION, resourceAddress);
        writeAttribute.get(NAME).set(RESOURCE_ATTRIBUTE.getName());
        writeAttribute.get(VALUE).set(incorrectValue);
        executeForFailure(writeAttribute);
        assertFalse("the notification handler did not receive the " + ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION, notificationEmittedLatch.await(250, MILLISECONDS));
    }

    @Test
    public void test_ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION_NotSentWhenAttributeValueIsTheSame() throws Exception {
        long value = System.currentTimeMillis();
        ModelNode add = createOperation(ADD, resourceAddress);
        add.get(RESOURCE_ATTRIBUTE.getName()).set(value);
        executeForResult(add);

        final CountDownLatch notificationEmittedLatch = new CountDownLatch(1);
        getController().getNotificationSupport().registerNotificationHandler(resourceAddressPattern, new NotificationHandler() {
                    @Override
                    public void handleNotification(Notification notif) {
                        notificationEmittedLatch.countDown();
                    }
                }, new NotificationFilter() {

                    @Override
                    public boolean isNotificationEnabled(Notification notification) {
                        return ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION.equals(notification.getType()) &&
                                resourceAddress.equals(pathAddress(notification.getResource()));
                    }
                });

        ModelNode writeAttribute = createOperation(WRITE_ATTRIBUTE_OPERATION, resourceAddress);
        writeAttribute.get(NAME).set(RESOURCE_ATTRIBUTE.getName());
        writeAttribute.get(VALUE).set(value);
        executeForResult(writeAttribute);
        assertFalse("the notification handler did receive the " + ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION, notificationEmittedLatch.await(250, MILLISECONDS));
    }

    @Test
    public void test_ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION_WhenUndefineAttributeIsCalled() throws Exception {

        long newValue = System.currentTimeMillis();

        executeForResult(createOperation(ADD, resourceAddress));

        // 2 notifications: 1 for the write-attribute to set the value and 1 for the undefine-attribute
        final CountDownLatch notificationEmittedLatch = new CountDownLatch(2);
        getController().getNotificationSupport().registerNotificationHandler(resourceAddressPattern, new NotificationHandler() {
                    @Override
                    public void handleNotification(Notification notif) {
                        notificationEmittedLatch.countDown();
                    }
                }, new NotificationFilter() {

                    @Override
                    public boolean isNotificationEnabled(Notification notification) {
                        return ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION.equals(notification.getType()) &&
                                resourceAddress.equals(pathAddress(notification.getResource()));
                    }
                });

        ModelNode writeAttribute = createOperation(WRITE_ATTRIBUTE_OPERATION, resourceAddress);
        writeAttribute.get(NAME).set(RESOURCE_ATTRIBUTE.getName());
        writeAttribute.get(VALUE).set(newValue);
        executeForResult(writeAttribute);

        ModelNode undefineAttribute = createOperation(UNDEFINE_ATTRIBUTE_OPERATION, resourceAddress);
        undefineAttribute.get(NAME).set(RESOURCE_ATTRIBUTE.getName());
        executeForResult(undefineAttribute);

        assertTrue("the notification handler did not receive the " + ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION, notificationEmittedLatch.await(1, SECONDS));
    }
}

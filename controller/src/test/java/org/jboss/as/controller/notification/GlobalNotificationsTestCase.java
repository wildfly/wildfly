/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.LONG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.global.GlobalNotifications;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.test.AbstractControllerTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class GlobalNotificationsTestCase extends AbstractControllerTestBase {

    public static final SimpleAttributeDefinition MY_ATTRIBUTE = create("my-attribute", LONG)
            .setDefaultValue(new ModelNode(12345))
            .build();
    public static final SimpleAttributeDefinition FAIL_ADD_OPERATION = create("fail-add-operation", BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .build();
    public static final SimpleAttributeDefinition FAIL_REMOVE_OPERATION = create("fail-remove-operation", BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .build();

    private static final PathAddress RESOURCE_ADDRESS_PATTERN = pathAddress(pathElement("profile", "*"));
    private final PathAddress resourceAddress = pathAddress(pathElement("profile", "myprofile"));

    @Override
    protected void initModel(Resource rootResource, final ManagementResourceRegistration rootRegistration) {
        // register the global operations to be able to call :read-attribute and :write-attribute
        GlobalOperationHandlers.registerGlobalOperations(rootRegistration, processType);
        // register the global notifications so there is no warning that emitted notifications are not described by the resource.
        GlobalNotifications.registerGlobalNotifications(rootRegistration, processType);

        ResourceDefinition profileDefinition = createDummyProfileResourceDefinition();
        rootRegistration.registerSubModel(profileDefinition);
    }

    private static ResourceDefinition createDummyProfileResourceDefinition() {
        return ResourceBuilder.Factory.create(RESOURCE_ADDRESS_PATTERN.getElement(0),
                new NonResolvingResourceDescriptionResolver())
                .setAddOperation(new AbstractAddStepHandler() {

                    @Override
                    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
                        MY_ATTRIBUTE.validateAndSet(operation, model);
                        FAIL_ADD_OPERATION.validateAndSet(operation, model);
                        FAIL_REMOVE_OPERATION.validateAndSet(operation, model);
                    }

                    @Override
                    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
                        boolean fail = FAIL_ADD_OPERATION.resolveModelAttribute(context, model).asBoolean();
                        if (fail) {
                            throw new OperationFailedException("add operation failed");
                        }
                    }
                })
                .setRemoveOperation(new AbstractRemoveStepHandler() {
                    @Override
                    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
                        boolean fail = FAIL_REMOVE_OPERATION.resolveModelAttribute(context, model).asBoolean();
                        if (fail) {
                            throw new OperationFailedException("remove operation failed");
                        }
                    }
                    // no-op
                })
                .addReadWriteAttribute(MY_ATTRIBUTE, null, new AbstractWriteAttributeHandler<Long>(MY_ATTRIBUTE) {
                    @Override
                    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Long> handbackHolder) throws OperationFailedException {
                        return false;
                    }

                    @Override
                    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Long handback) throws OperationFailedException {
                    }
                })
                .build();
    }

    @Test
    public void test_RESOURCE_ADDED_NOTIFICATION() throws Exception {
        ListBackedNotificationHandler handler = new ListBackedNotificationHandler();
        NotificationFilter filter = new TestNotificationHandler(resourceAddress, RESOURCE_ADDED_NOTIFICATION);
        getController().getNotificationRegistry().registerNotificationHandler(RESOURCE_ADDRESS_PATTERN, handler, filter);

        executeForResult(createOperation(ADD, resourceAddress));
        assertEquals("the notification handler did not receive the " + RESOURCE_ADDED_NOTIFICATION, 1, handler.getNotifications().size());

        getController().getNotificationRegistry().unregisterNotificationHandler(RESOURCE_ADDRESS_PATTERN, handler, filter);
    }

    @Test
    public void test_RESOURCE_ADDED_NOTIFICATION_isNotSentWhenAddOperationFails() throws Exception {
        ListBackedNotificationHandler handler = new ListBackedNotificationHandler();
        NotificationFilter filter = new TestNotificationHandler(resourceAddress, RESOURCE_ADDED_NOTIFICATION);
        getController().getNotificationRegistry().registerNotificationHandler(RESOURCE_ADDRESS_PATTERN, handler, filter);

        ModelNode addOperation = createOperation(ADD, resourceAddress);
        addOperation.get(FAIL_ADD_OPERATION.getName()).set(true);
        executeForFailure(addOperation);

        assertTrue("the notification handler unexpectedly receives the " + RESOURCE_ADDED_NOTIFICATION, handler.getNotifications().isEmpty());

        getController().getNotificationRegistry().unregisterNotificationHandler(RESOURCE_ADDRESS_PATTERN, handler, filter);
    }

    @Test
    public void test_RESOURCE_REMOVED_NOTIFICATION() throws Exception {
        executeForResult(createOperation(ADD, resourceAddress));

        ListBackedNotificationHandler handler = new ListBackedNotificationHandler();
        NotificationFilter filter = new TestNotificationHandler(resourceAddress, RESOURCE_REMOVED_NOTIFICATION);
        getController().getNotificationRegistry().registerNotificationHandler(RESOURCE_ADDRESS_PATTERN, handler, filter);

        executeForResult(createOperation(REMOVE, resourceAddress));
        assertEquals("the notification handler did not receive the " + RESOURCE_REMOVED_NOTIFICATION, 1, handler.getNotifications().size());

        getController().getNotificationRegistry().unregisterNotificationHandler(RESOURCE_ADDRESS_PATTERN, handler, filter);
    }

    @Test
    public void test_RESOURCE_REMOVED_NOTIFICATION_isNotSentWhenRemoveOperationFails() throws Exception {
        ModelNode addOperation = createOperation(ADD, resourceAddress);
        addOperation.get(FAIL_REMOVE_OPERATION.getName()).set(true);
        executeForResult(addOperation);

        ListBackedNotificationHandler handler = new ListBackedNotificationHandler();
        NotificationFilter filter = new TestNotificationHandler(resourceAddress, RESOURCE_REMOVED_NOTIFICATION);
        getController().getNotificationRegistry().registerNotificationHandler(RESOURCE_ADDRESS_PATTERN, handler, filter);

        executeForFailure(createOperation(REMOVE, resourceAddress));
        assertTrue("the notification handler unexpectedly receives the " + RESOURCE_REMOVED_NOTIFICATION, handler.getNotifications().isEmpty());

        getController().getNotificationRegistry().unregisterNotificationHandler(RESOURCE_ADDRESS_PATTERN, handler, filter);
    }

    @Test
    public void test_ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION() throws Exception {
        executeForResult(createOperation(ADD, resourceAddress));

        ModelNode readAttribute = createOperation(READ_ATTRIBUTE_OPERATION, resourceAddress);
        readAttribute.get(NAME).set(MY_ATTRIBUTE.getName());
        ModelNode result = executeForResult(readAttribute);
        // read-attribute returns the default value
        assertEquals(MY_ATTRIBUTE.getDefaultValue().asLong(), result.asLong());

        ListBackedNotificationHandler handler = new ListBackedNotificationHandler();
        NotificationFilter filter = new TestNotificationHandler(resourceAddress, ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION);
        getController().getNotificationRegistry().registerNotificationHandler(RESOURCE_ADDRESS_PATTERN, handler, filter);

        long newValue = System.currentTimeMillis();
        ModelNode writeAttribute = createOperation(WRITE_ATTRIBUTE_OPERATION, resourceAddress);
        writeAttribute.get(NAME).set(MY_ATTRIBUTE.getName());
        writeAttribute.get(VALUE).set(newValue);
        executeForResult(writeAttribute);

        assertEquals("the notification handler did not receive the " + ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION, 1, handler.getNotifications().size());
        Notification notification = handler.getNotifications().get(0);
        assertEquals(MY_ATTRIBUTE.getName(), notification.getData().require(NAME).asString());
        // the value was not defined initially: the notification does not return the default value but undefined instead.
        assertFalse(notification.getData().require(OLD_VALUE).isDefined());
        assertEquals(newValue, notification.getData().require(NEW_VALUE).asLong());

        getController().getNotificationRegistry().unregisterNotificationHandler(RESOURCE_ADDRESS_PATTERN, handler, filter);
    }

    @Test
    public void test_ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION_isNotSentWhenWriteAttributeOperationFails() throws Exception {

        executeForResult(createOperation(ADD, resourceAddress));

        ListBackedNotificationHandler handler = new ListBackedNotificationHandler();
        NotificationFilter filter = new TestNotificationHandler(resourceAddress, ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION);
        getController().getNotificationRegistry().registerNotificationHandler(RESOURCE_ADDRESS_PATTERN, handler, filter);

        String incorrectValue = UUID.randomUUID().toString();
        ModelNode writeAttribute = createOperation(WRITE_ATTRIBUTE_OPERATION, resourceAddress);
        writeAttribute.get(NAME).set(MY_ATTRIBUTE.getName());
        writeAttribute.get(VALUE).set(incorrectValue);
        executeForFailure(writeAttribute);
        assertTrue("the notification handler unexpectedly receives the " + ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION, handler.getNotifications().isEmpty());

        getController().getNotificationRegistry().unregisterNotificationHandler(RESOURCE_ADDRESS_PATTERN, handler, filter);
    }

    @Test
    public void test_ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION_isNotSentWhenAttributeValueIsTheSame() throws Exception {
        long value = System.currentTimeMillis();
        ModelNode add = createOperation(ADD, resourceAddress);
        add.get(MY_ATTRIBUTE.getName()).set(value);
        executeForResult(add);

        ListBackedNotificationHandler handler = new ListBackedNotificationHandler();
        NotificationFilter filter = new TestNotificationHandler(resourceAddress, ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION);
        getController().getNotificationRegistry().registerNotificationHandler(RESOURCE_ADDRESS_PATTERN, handler, filter);

        ModelNode writeAttribute = createOperation(WRITE_ATTRIBUTE_OPERATION, resourceAddress);
        writeAttribute.get(NAME).set(MY_ATTRIBUTE.getName());
        writeAttribute.get(VALUE).set(value);
        executeForResult(writeAttribute);
        assertEquals("the notification handler unexpectedly receives the " + ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION, 0, handler.getNotifications().size());

        getController().getNotificationRegistry().unregisterNotificationHandler(RESOURCE_ADDRESS_PATTERN, handler, filter);
    }

    @Test
    public void test_ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION_isSentWhenUndefineAttributeIsCalled() throws Exception {
        long initialValue = System.currentTimeMillis();
        ModelNode add = createOperation(ADD, resourceAddress);
        add.get(MY_ATTRIBUTE.getName()).set(initialValue);
        executeForResult(add);

        ListBackedNotificationHandler handler = new ListBackedNotificationHandler();
        NotificationFilter filter = new TestNotificationHandler(resourceAddress, ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION);
        getController().getNotificationRegistry().registerNotificationHandler(RESOURCE_ADDRESS_PATTERN, handler, filter);

        ModelNode undefineAttribute = createOperation(UNDEFINE_ATTRIBUTE_OPERATION, resourceAddress);
        undefineAttribute.get(NAME).set(MY_ATTRIBUTE.getName());
        executeForResult(undefineAttribute);

        assertEquals("the notification handler did not receive the " + ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION, 1, handler.getNotifications().size());
        Notification notification = handler.getNotifications().get(0);
        assertEquals(MY_ATTRIBUTE.getName(), notification.getData().require(NAME).asString());
        assertEquals(initialValue, notification.getData().require(OLD_VALUE).asLong());
        assertFalse(notification.getData().require(NEW_VALUE).isDefined());

        getController().getNotificationRegistry().unregisterNotificationHandler(RESOURCE_ADDRESS_PATTERN, handler, filter);
    }

    @Test
    public void test_ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION_isSentWhenUndefineAttributeIsCalledOnAnUndefinedAttribute() throws Exception {
        // MY_ATTRIBUTE is not defined when the resource is created.
        ModelNode add = createOperation(ADD, resourceAddress);
        executeForResult(add);

        ListBackedNotificationHandler handler = new ListBackedNotificationHandler();
        NotificationFilter filter = new TestNotificationHandler(resourceAddress, ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION);
        getController().getNotificationRegistry().registerNotificationHandler(RESOURCE_ADDRESS_PATTERN, handler, filter);

        ModelNode undefineAttribute = createOperation(UNDEFINE_ATTRIBUTE_OPERATION, resourceAddress);
        undefineAttribute.get(NAME).set(MY_ATTRIBUTE.getName());
        executeForResult(undefineAttribute);

        assertTrue("the notification handler unexpectedly receives the " + ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION, handler.getNotifications().isEmpty());

        getController().getNotificationRegistry().unregisterNotificationHandler(RESOURCE_ADDRESS_PATTERN, handler, filter);
    }

    private static class TestNotificationHandler implements NotificationFilter {

        private final PathAddress expectedAddress;
        private final String expectedType;

        /**
         * Filters out notifications so that the handler will handle only those that are from the {@code expectedType}
         * and emitted from the {@code expectedAddress}.
         */
        TestNotificationHandler(PathAddress expectedAddress, String expectedType) {

            this.expectedAddress = expectedAddress;
            this.expectedType = expectedType;
        }

        @Override
        public boolean isNotificationEnabled(Notification notification) {
            return notification.getSource().equals(expectedAddress) &&
                    notification.getType().equals(expectedType);
        }
    }
}
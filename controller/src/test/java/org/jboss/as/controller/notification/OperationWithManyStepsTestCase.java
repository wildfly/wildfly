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

import static org.jboss.as.controller.OperationContext.Stage.RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.notification.NotificationFilter.ALL;
import static org.jboss.as.controller.notification.NotificationRegistry.ANY_ADDRESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.test.AbstractControllerTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Test;

/**
 * Test emitting notifications for an operation composed of many steps.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class OperationWithManyStepsTestCase extends AbstractControllerTestBase {

    private static final String MY_OPERATION = "my-operation";
    private static final String MY_NOTIFICATION_TYPE = "my-notification-type";

    private static final String MESSAGE1 = "first notification message";
    private static final String MESSAGE2 = "second notification message";

    private static final SimpleAttributeDefinition FAIL_FIRST_STEP = SimpleAttributeDefinitionBuilder.create("fail-first-step", ModelType.BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(false))
            .build();
    private static final AttributeDefinition FAIL_SECOND_STEP = SimpleAttributeDefinitionBuilder.create("fail-second-step", FAIL_FIRST_STEP)
            .build();
    private static final SimpleAttributeDefinition ROLLBACK = SimpleAttributeDefinitionBuilder.create("rollback", ModelType.BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(false))
            .build();

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        registration.registerOperationHandler(new SimpleOperationDefinitionBuilder(MY_OPERATION, new NonResolvingResourceDescriptionResolver())
                        .setParameters(FAIL_FIRST_STEP, FAIL_SECOND_STEP)
                        .setPrivateEntry()
                        .build(),
                new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        boolean failFirstStep = FAIL_FIRST_STEP.resolveModelAttribute(context, operation).asBoolean();
                        boolean rollback = ROLLBACK.resolveModelAttribute(context, operation).asBoolean();

                        System.out.println("send 1st notification");
                        context.emit(new Notification(MY_NOTIFICATION_TYPE, PathAddress.pathAddress(operation.get(OP_ADDR)), MESSAGE1));

                        if (failFirstStep) {
                            throw new OperationFailedException("1st step failed");
                        }

                        context.addStep(new OperationStepHandler() {
                            @Override
                            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                                boolean failSecondStep = FAIL_SECOND_STEP.resolveModelAttribute(context, operation).asBoolean();

                                System.out.println("send 2nd notification");
                                context.emit(new Notification(MY_NOTIFICATION_TYPE, PathAddress.pathAddress(operation.get(OP_ADDR)), MESSAGE2));

                                if (failSecondStep) {
                                    throw new OperationFailedException("2nd step failed");
                                }

                                context.stepCompleted();
                            }
                        }, RUNTIME);

                        if (rollback) {
                            context.getFailureDescription().set("rolled back");
                            context.setRollbackOnly();
                        }
                        context.stepCompleted();
                    }
                }
        );
    }

    @Test
    public void testSendNotificationForSuccessfulOperation() throws Exception {
        ListBackedNotificationHandler handler = new ListBackedNotificationHandler();

        // register the handler...
        getController().getNotificationRegistry().registerNotificationHandler(ANY_ADDRESS, handler, ALL);
        executeForResult(createOperation(MY_OPERATION));

        assertEquals("the notification handler did not receive the notifications", 2, handler.getNotifications().size());
        // notifications are received in the order they were emitted
        assertEquals(MESSAGE1, handler.getNotifications().get(0).getMessage());
        assertEquals(MESSAGE2, handler.getNotifications().get(1).getMessage());

        getController().getNotificationRegistry().unregisterNotificationHandler(ANY_ADDRESS, handler, ALL);
    }


    @Test
    public void testSendNotificationForFailingOperationInFirstStep() throws Exception {
        ListBackedNotificationHandler handler = new ListBackedNotificationHandler();
        getController().getNotificationRegistry().registerNotificationHandler(ANY_ADDRESS, handler, ALL);

        ModelNode operation = createOperation(MY_OPERATION);
        operation.get(FAIL_FIRST_STEP.getName()).set(true);

        try {
            executeForResult(operation);
            fail("operation must fail");
        } catch (OperationFailedException e) {
            assertEquals("1st step failed", e.getFailureDescription().asString());
        }

        assertTrue("the notification handler did not receive any notifications", handler.getNotifications().isEmpty());

        getController().getNotificationRegistry().unregisterNotificationHandler(ANY_ADDRESS, handler, ALL);
    }


    @Test
    public void testSendNotificationForFailingOperationInSecondStep() throws Exception {
        ListBackedNotificationHandler handler = new ListBackedNotificationHandler();
        getController().getNotificationRegistry().registerNotificationHandler(ANY_ADDRESS, handler, ALL);

        ModelNode operation = createOperation(MY_OPERATION);
        operation.get(FAIL_SECOND_STEP.getName()).set(true);
        try {
            executeForResult(operation);
            fail("operation must fail");
        } catch (OperationFailedException e) {
            assertEquals("2nd step failed", e.getFailureDescription().asString());
        }

        assertTrue("the notification handler did not receive any notifications", handler.getNotifications().isEmpty());

        getController().getNotificationRegistry().unregisterNotificationHandler(ANY_ADDRESS, handler, ALL);
    }

    @Test
    public void testSendNotificationForRollingBackOperation() throws Exception {
        ListBackedNotificationHandler handler = new ListBackedNotificationHandler();
        getController().getNotificationRegistry().registerNotificationHandler(ANY_ADDRESS, handler, ALL);

        ModelNode operation = createOperation(MY_OPERATION);
        operation.get(ROLLBACK.getName()).set(true);
        try {
            executeForResult(operation);
            fail("operation must have been rolled back");
        } catch (OperationFailedException e) {
            assertEquals("rolled back", e.getFailureDescription().asString());
        }

        assertTrue("the notification handler did not receive any notifications", handler.getNotifications().isEmpty());

        getController().getNotificationRegistry().unregisterNotificationHandler(ANY_ADDRESS, handler, ALL);
    }
}

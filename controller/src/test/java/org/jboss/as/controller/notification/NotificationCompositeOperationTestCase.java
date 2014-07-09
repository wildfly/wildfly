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

import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.notification.NotificationFilter.ALL;
import static org.jboss.as.controller.notification.NotificationRegistry.ANY_ADDRESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jboss.as.controller.CompositeOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.test.AbstractControllerTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class NotificationCompositeOperationTestCase extends AbstractControllerTestBase {

    private static final String MY_OPERATION = "my-operation";
    private static final String MY_NOTIFICATION_TYPE = "my-notification-type";

    private static final SimpleAttributeDefinition FAIL_OPERATION = SimpleAttributeDefinitionBuilder.create("fail-operation", ModelType.BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(false))
            .build();

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        registration.registerOperationHandler(CompositeOperationHandler.DEFINITION, CompositeOperationHandler.INSTANCE);

        registration.registerOperationHandler(new SimpleOperationDefinitionBuilder(MY_OPERATION, new NonResolvingResourceDescriptionResolver())
                        .setParameters(FAIL_OPERATION)
                .setPrivateEntry()
                .build(),
                new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        Notification notification = new Notification(MY_NOTIFICATION_TYPE, pathAddress(operation.get(OP_ADDR)), operation.get("param").asString());
                        context.emit(notification);

                        boolean failOperation = FAIL_OPERATION.resolveModelAttribute(context, operation).asBoolean();
                        if (failOperation) {
                            throw new OperationFailedException("failed operation");
                        }
                        context.stepCompleted();
                    }
                }
        );
    }

    @Test
    public void testCompositeOperationThatEmitsNotifications() throws Exception {
        ListBackedNotificationHandler handler = new ListBackedNotificationHandler();
        getController().getNotificationRegistry().registerNotificationHandler(ANY_ADDRESS, handler, ALL);

        ModelNode operation = new ModelNode();
        operation.get(OP).set("composite");
        operation.get(OP_ADDR).setEmptyList();
        ModelNode op1 = createOperation(MY_OPERATION);
        op1.get("param").set("param1");
        ModelNode op2 = createOperation(MY_OPERATION);
        op2.get("param").set("param2");
        operation.get(ModelDescriptionConstants.STEPS).add(op1);
        operation.get(ModelDescriptionConstants.STEPS).add(op2);
        ModelNode result = executeForResult(operation);
        System.out.println("operation = " + operation);
        System.out.println("result = " + result);

        // the notifications are received in the order they were emitted
        assertEquals("the notification were not emitted", 2, handler.getNotifications().size());
        assertEquals("param1", handler.getNotifications().get(0).getMessage());
        assertEquals("param2", handler.getNotifications().get(1).getMessage());

        getController().getNotificationRegistry().unregisterNotificationHandler(ANY_ADDRESS, handler, ALL);
    }

    @Test
    public void testCompositeOperationWithFirstOperationFailing() throws Exception {
        ListBackedNotificationHandler handler = new ListBackedNotificationHandler();
        getController().getNotificationRegistry().registerNotificationHandler(ANY_ADDRESS, handler, ALL);

        ModelNode operation = new ModelNode();
        operation.get(OP).set("composite");
        operation.get(OP_ADDR).setEmptyList();
        ModelNode op1 = createOperation(MY_OPERATION);
        op1.get(FAIL_OPERATION.getName()).set(true);
        op1.get("param").set("param1");
        ModelNode op2 = createOperation(MY_OPERATION);
        op2.get("param").set("param2");
        operation.get(ModelDescriptionConstants.STEPS).add(op1);
        operation.get(ModelDescriptionConstants.STEPS).add(op2);

        executeForFailure(operation);

        assertTrue("the notification were emitted", handler.getNotifications().isEmpty());

        getController().getNotificationRegistry().unregisterNotificationHandler(ANY_ADDRESS, handler, ALL);
    }

    @Test
    public void testCompositeOperationWithSecondOperationFailing() throws Exception {
        ListBackedNotificationHandler handler = new ListBackedNotificationHandler();
        getController().getNotificationRegistry().registerNotificationHandler(ANY_ADDRESS, handler, ALL);

        ModelNode operation = new ModelNode();
        operation.get(OP).set("composite");
        operation.get(OP_ADDR).setEmptyList();
        ModelNode op1 = createOperation(MY_OPERATION);
        op1.get("param").set("param1");
        ModelNode op2 = createOperation(MY_OPERATION);
        op2.get("param").set("param2");
        op2.get(FAIL_OPERATION.getName()).set(true);
        operation.get(ModelDescriptionConstants.STEPS).add(op1);
        operation.get(ModelDescriptionConstants.STEPS).add(op2);

        executeForFailure(operation);

        assertTrue("the notification were emitted", handler.getNotifications().isEmpty());

        getController().getNotificationRegistry().unregisterNotificationHandler(ANY_ADDRESS, handler, ALL);
    }

}

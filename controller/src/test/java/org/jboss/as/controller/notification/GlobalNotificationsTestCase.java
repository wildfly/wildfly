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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_ADDED_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_REMOVED_NOTIFICATION;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.test.AbstractControllerTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class GlobalNotificationsTestCase extends AbstractControllerTestBase {

    private static final OperationDefinition SETUP_OP_DEF = new SimpleOperationDefinitionBuilder("setup", new NonResolvingResourceDescriptionResolver())
            .setPrivateEntry()
            .build();
    public static final String OPERATION_THAT_REGISTERS_A_NOTIFICATION_HANDLER = "operation-that-registers-a-notification-handler";

    public static final AtomicReference<CountDownLatch> notificationEmittedLatch = new AtomicReference<>();
    public static final AtomicReference<CountDownLatch> notificationHandlerRegisteredLatch = new AtomicReference<>();
    public static final AtomicReference<CountDownLatch> notificationHandlerUnregisteredLatch = new AtomicReference<>();
    public static final AtomicReference<NotificationHandler> notificationHandler = new AtomicReference<>();
    public static final AtomicReference<NotificationFilter> notificationFilter = new AtomicReference<>();

    private static final OperationDefinition ADD_RESOURCE = new SimpleOperationDefinitionBuilder(ADD, new NonResolvingResourceDescriptionResolver())
            .setPrivateEntry()
            .build();
    private static final OperationDefinition REMOVE_RESOURCE = new SimpleOperationDefinitionBuilder(REMOVE, new NonResolvingResourceDescriptionResolver())
            .setPrivateEntry()
            .build();

    @Override
    protected void initModel(Resource rootResource, final ManagementResourceRegistration rootRegistration) {
        notificationEmittedLatch.set(new CountDownLatch(1));
        notificationHandlerRegisteredLatch.set(new CountDownLatch(1));
        notificationHandlerUnregisteredLatch.set(new CountDownLatch(1));

        notificationHandler.set(new NotificationHandler() {
            @Override
            public void handleNotification(Notification notification) {
                notificationEmittedLatch.get().countDown();
            }
        });
        notificationFilter.set(NotificationFilter.ALL);

        NotificationService.installNotificationService(getContainer().subTarget());

        rootRegistration.registerOperationHandler(SETUP_OP_DEF, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                ModelNode resource = new ModelNode();
                resource.add("profile", "*");
                ManagementResourceRegistration subModelRegistry = rootRegistration.getSubModel(pathAddress(resource));
                subModelRegistry.registerOperationHandler(ADD_RESOURCE, new AbstractAddStepHandler() {
                    @Override
                    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
                    }
                });
                subModelRegistry.registerOperationHandler(REMOVE_RESOURCE, new AbstractRemoveStepHandler() {
                    // no-op
                });
                context.stepCompleted();
            }
        });
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

        ResourceDefinition profileDef = ResourceBuilder.Factory.create(pathElement("profile", "*"),
                new NonResolvingResourceDescriptionResolver())
                .addReadOnlyAttribute(SimpleAttributeDefinitionBuilder.create("name", ModelType.STRING, false).setMinSize(1).build())
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
}

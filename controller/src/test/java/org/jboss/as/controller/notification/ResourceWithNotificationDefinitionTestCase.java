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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NOTIFICATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NOTIFICATION_DATA_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NOTIFICATION_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.NotificationDefinition;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.test.AbstractControllerTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class ResourceWithNotificationDefinitionTestCase extends AbstractControllerTestBase {

    private static final String MY_TYPE = "my-notification-type";
    private static final String NOTIFICATION_DESCRIPTION = "My Notification Description";
    private static final ModelNode DATA_TYPE_DESCRIPTION;

    static {
        DATA_TYPE_DESCRIPTION = new ModelNode();
        DATA_TYPE_DESCRIPTION.get("foo", DESCRIPTION).set("description of foo");
        DATA_TYPE_DESCRIPTION.get("bar", DESCRIPTION).set("description of bar");

    }

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        GlobalOperationHandlers.registerGlobalOperations(registration, ProcessType.STANDALONE_SERVER);
        NotificationDefinition notificationDefinition = NotificationDefinition.Builder.create(MY_TYPE,
                new NonResolvingResourceDescriptionResolver() {
                    @Override
                    public String getNotificationDescription(String notificationType, Locale locale, ResourceBundle bundle) {
                        return NOTIFICATION_DESCRIPTION;
                    }
                })
                .setDataValueDescriptor(new NotificationDefinition.DataValueDescriptor() {
                    @Override
                    public ModelNode describe(ResourceBundle bundle) {
                        return DATA_TYPE_DESCRIPTION;
                    }
                })
                .build();

        registration.registerNotification(notificationDefinition);
    }

    @Test
    public void testReadResourceDescriptionWithNotification() throws Exception {
        ModelNode readResourceDescription = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        readResourceDescription.get(NOTIFICATIONS).set(true);

        ModelNode description = executeForResult(readResourceDescription);
        assertTrue(description.hasDefined(NOTIFICATIONS));

        List<Property> notifications = description.require(NOTIFICATIONS).asPropertyList();
        assertEquals(1, notifications.size());
        Property notification = notifications.get(0);
        assertEquals(MY_TYPE, notification.getName());
        assertEquals(MY_TYPE, notification.getValue().get(NOTIFICATION_TYPE).asString());
        assertEquals(NOTIFICATION_DESCRIPTION, notification.getValue().get(DESCRIPTION).asString());
        assertEquals(DATA_TYPE_DESCRIPTION, notification.getValue().get(NOTIFICATION_DATA_TYPE));
    }
}

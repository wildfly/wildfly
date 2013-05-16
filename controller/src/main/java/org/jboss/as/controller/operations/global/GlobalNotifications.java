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

package org.jboss.as.controller.operations.global;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_ADDED_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_REMOVED_NOTIFICATION;

import java.util.ResourceBundle;

import org.jboss.as.controller.NotificationDefinition;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Global notifications emitted by all resources.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class GlobalNotifications {

    private static final NotificationDefinition RESOURCE_ADDED = NotificationDefinition.Builder.create(RESOURCE_ADDED_NOTIFICATION, ControllerResolver.getResolver("global"))
            .build();
    private static final NotificationDefinition RESOURCE_REMOVED = NotificationDefinition.Builder.create(RESOURCE_REMOVED_NOTIFICATION, ControllerResolver.getResolver("global"))
            .build();

    public static final String OLD_VALUE = "old-value";
    public static final String NEW_VALUE = "new-value";

    private static final NotificationDefinition ATTRIBUTE_VALUE_WRITTEN = NotificationDefinition.Builder.create(ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION, ControllerResolver.getResolver("global"))
            .setDataValueDescriptor(new NotificationDefinition.DataValueDescriptor() {
                @Override
                public ModelNode describe(ResourceBundle bundle) {
                    String prefix = "global." + ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION + ".";
                    final ModelNode desc = new ModelNode();
                    desc.get(NAME, DESCRIPTION).set(bundle.getString(prefix + NAME));
                    desc.get(OLD_VALUE, DESCRIPTION).set(bundle.getString(prefix + OLD_VALUE));
                    desc.get(NEW_VALUE, DESCRIPTION).set(bundle.getString(prefix + NEW_VALUE));
                    return desc;
                }
            })
            .build();

    public static void registerGlobalNotifications(ManagementResourceRegistration root, ProcessType processType) {
        root.registerNotification(RESOURCE_ADDED, true);
        root.registerNotification(RESOURCE_REMOVED, true);

        if (processType != ProcessType.DOMAIN_SERVER) {
            root.registerNotification(ATTRIBUTE_VALUE_WRITTEN, true);
        }
    }
}

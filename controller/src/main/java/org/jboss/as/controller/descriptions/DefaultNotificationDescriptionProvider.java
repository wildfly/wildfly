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

package org.jboss.as.controller.descriptions;

import static org.jboss.as.controller.NotificationDefinition.DataValueDescriptor;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NOTIFICATION_DATA_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NOTIFICATION_TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.dmr.ModelNode;

/**
 * Provides a default description of a notification.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat Inc.
 */
public class DefaultNotificationDescriptionProvider implements DescriptionProvider {

    private final String notificationType;
    private final ResourceDescriptionResolver descriptionResolver;
    private final DataValueDescriptor dataValueDescriptor;

    public DefaultNotificationDescriptionProvider(final String notificationType,
                                                  final ResourceDescriptionResolver descriptionResolver,
                                                  final DataValueDescriptor dataValueDescriptor) {
        this.notificationType = notificationType;
        this.descriptionResolver = descriptionResolver;
        this.dataValueDescriptor = dataValueDescriptor;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        ModelNode result = new ModelNode();

        final ResourceBundle bundle = descriptionResolver.getResourceBundle(locale);
        result.get(NOTIFICATION_TYPE).set(notificationType);
        result.get(DESCRIPTION).set(descriptionResolver.getNotificationDescription(notificationType, locale, bundle));
        if (dataValueDescriptor != null) {
            ModelNode dataDescription = dataValueDescriptor.describe(bundle);
            if (dataDescription != null && dataDescription.isDefined()) {
                result.get(NOTIFICATION_DATA_TYPE).set(dataDescription);
            }
        }

        return result;
    }
}

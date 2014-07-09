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

package org.jboss.as.controller;

import static org.jboss.as.controller.NotificationDefinition.DataValueDescriptor.NO_DATA;

import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DefaultNotificationDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.dmr.ModelNode;

/**
 * Defining characteristics of notification in a {@link org.jboss.as.controller.registry.Resource}
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class NotificationDefinition {

    private final String type;
    private final ResourceDescriptionResolver resolver;
    private final DataValueDescriptor dataValueDescriptor;

    private NotificationDefinition(final String type, final ResourceDescriptionResolver resolver, final DataValueDescriptor dataValueDescriptor) {
        this.type = type;
        this.resolver = resolver;
        this.dataValueDescriptor = dataValueDescriptor;
    }

    public String getType() {
        return type;
    }

    public DescriptionProvider getDescriptionProvider() {
        return new DefaultNotificationDescriptionProvider(type, resolver, dataValueDescriptor);
    }

    public static class Builder {
        private final String type;
        private final ResourceDescriptionResolver resolver;
        private DataValueDescriptor dataValueDescriptor = NO_DATA;

        private Builder(String type, ResourceDescriptionResolver resolver) {
            this.type = type;
            this.resolver = resolver;
        }

        public static Builder create(String type, ResourceDescriptionResolver resolver) {
            return new Builder(type, resolver);
        }

        public Builder setDataValueDescriptor(DataValueDescriptor dataValueDescriptor) {
            this.dataValueDescriptor = dataValueDescriptor;
            return this;
        }

        public NotificationDefinition build() {
            return new NotificationDefinition(type, resolver, dataValueDescriptor);
        }
    }

    public interface DataValueDescriptor {
        ModelNode describe(ResourceBundle bundle);

        DataValueDescriptor NO_DATA = new DataValueDescriptor() {
            @Override
            public ModelNode describe(ResourceBundle bundle) {
                return null;
            }
        };
    }
}

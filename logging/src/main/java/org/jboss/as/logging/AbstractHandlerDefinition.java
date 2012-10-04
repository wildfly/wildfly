/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DISABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLE;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FILTER;
import static org.jboss.as.logging.CommonAttributes.FILTER_SPEC;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.NAME;
import static org.jboss.as.logging.HandlerOperations.HandlerUpdateOperationStepHandler;
import static org.jboss.as.logging.HandlerOperations.LogHandlerWriteAttributeHandler;

import java.util.logging.Handler;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReadResourceNameOperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.logging.HandlerOperations.HandlerAddOperationStepHandler;
import org.jboss.as.logging.LoggingOperations.ReadFilterOperationStepHandler;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
abstract class AbstractHandlerDefinition extends SimpleResourceDefinition {

    public static final String UPDATE_OPERATION_NAME = "update-properties";
    public static final String CHANGE_LEVEL_OPERATION_NAME = "change-log-level";

    static final AttributeDefinition[] DEFAULT_ATTRIBUTES = {
            LEVEL,
            ENCODING,
            FORMATTER,
            FILTER_SPEC,
    };

    static final AttributeDefinition[] LEGACY_ATTRIBUTES = {
            FILTER,
    };

    private final LogHandlerWriteAttributeHandler writeHandler;
    private final AttributeDefinition[] writableAttributes;
    private final AttributeDefinition[] readOnlyAttributes;

    protected AbstractHandlerDefinition(final PathElement path,
                                        final Class<? extends Handler> type,
                                        final AttributeDefinition[] attributes) {
        this(path, type, attributes, null, attributes);
    }

    protected AbstractHandlerDefinition(final PathElement path,
                                        final Class<? extends Handler> type,
                                        final AttributeDefinition[] attributes,
                                        final ConfigurationProperty<?>... constructionProperties) {
        this(path, type, attributes, null, attributes, constructionProperties);
    }

    protected AbstractHandlerDefinition(final PathElement path,
                                        final Class<? extends Handler> type,
                                        final AttributeDefinition[] addAttributes,
                                        final AttributeDefinition[] readOnlyAttributes,
                                        final AttributeDefinition[] writableAttributes,
                                        final ConfigurationProperty<?>... constructionProperties) {
        super(path,
                LoggingExtension.getResourceDescriptionResolver("handler"),
                new HandlerAddOperationStepHandler(type, addAttributes, constructionProperties),
                HandlerOperations.REMOVE_HANDLER);
        this.writableAttributes = writableAttributes;
        writeHandler = new LogHandlerWriteAttributeHandler(this.writableAttributes);
        this.readOnlyAttributes = readOnlyAttributes;
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition def : writableAttributes) {
            // Filter requires a special reader
            if (def.getName().equals(FILTER.getName())) {
                resourceRegistration.registerReadWriteAttribute(def, ReadFilterOperationStepHandler.INSTANCE, writeHandler);
            } else {
                resourceRegistration.registerReadWriteAttribute(def, null, writeHandler);
            }
        }
        if (readOnlyAttributes != null)
            for (AttributeDefinition def : readOnlyAttributes) {
                resourceRegistration.registerReadOnlyAttribute(def, null);
            }
        resourceRegistration.registerReadOnlyAttribute(NAME, ReadResourceNameOperationStepHandler.INSTANCE);
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration registration) {
        super.registerOperations(registration);
        final ResourceDescriptionResolver resolver = getResourceDescriptionResolver();
        registration.registerOperationHandler(new SimpleOperationDefinition(ENABLE, resolver), HandlerOperations.ENABLE_HANDLER);
        registration.registerOperationHandler(new SimpleOperationDefinition(DISABLE, resolver), HandlerOperations.DISABLE_HANDLER);
        registration.registerOperationHandler(new SimpleOperationDefinition(CHANGE_LEVEL_OPERATION_NAME, resolver, CommonAttributes.LEVEL), HandlerOperations.CHANGE_LEVEL);
        registration.registerOperationHandler(new SimpleOperationDefinition(UPDATE_OPERATION_NAME, resolver, writableAttributes), new HandlerUpdateOperationStepHandler(writableAttributes));
    }
}

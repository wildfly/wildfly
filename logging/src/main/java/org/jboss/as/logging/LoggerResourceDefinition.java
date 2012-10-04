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

import static org.jboss.as.logging.CommonAttributes.CATEGORY;
import static org.jboss.as.logging.CommonAttributes.FILTER;
import static org.jboss.as.logging.CommonAttributes.FILTER_SPEC;
import static org.jboss.as.logging.CommonAttributes.HANDLERS;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.USE_PARENT_HANDLERS;
import static org.jboss.as.logging.Logging.join;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.logging.LoggingOperations.ReadFilterOperationStepHandler;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class LoggerResourceDefinition extends SimpleResourceDefinition {
    public static final String CHANGE_LEVEL_OPERATION_NAME = "change-log-level";
    public static final String ADD_HANDLER_OPERATION_NAME = "assign-handler";
    public static final String REMOVE_HANDLER_OPERATION_NAME = "unassign-handler";
    static final PathElement LOGGER_PATH = PathElement.pathElement(CommonAttributes.LOGGER);

    static final AttributeDefinition[] ATTRIBUTES = {
            CATEGORY,
            FILTER_SPEC,
            LEVEL,
            HANDLERS,
            USE_PARENT_HANDLERS
    };

    static final AttributeDefinition[] WRITABLE_ATTRIBUTES = {
            FILTER_SPEC,
            LEVEL,
            HANDLERS,
            USE_PARENT_HANDLERS
    };

    static final AttributeDefinition[] LEGACY_ATTRIBUTES = {
            FILTER,
    };

    /**
     * A step handler to add a logger.
     */
    static LoggerOperations.LoggerAddOperationStepHandler ADD_LOGGER = new LoggerOperations.LoggerAddOperationStepHandler(LEGACY_ATTRIBUTES);

    private final AttributeDefinition[] writableAttributes;
    private final OperationStepHandler writeHandler;

    public LoggerResourceDefinition(final boolean includeLegacy) {
        super(LOGGER_PATH,
                LoggingExtension.getResourceDescriptionResolver(CommonAttributes.LOGGER),
                (includeLegacy ? new LoggerOperations.LoggerAddOperationStepHandler(join(ATTRIBUTES, LEGACY_ATTRIBUTES)) : new LoggerOperations.LoggerAddOperationStepHandler(ATTRIBUTES)),
                LoggerOperations.REMOVE_LOGGER);
        writableAttributes = (includeLegacy ? join(WRITABLE_ATTRIBUTES, LEGACY_ATTRIBUTES) : WRITABLE_ATTRIBUTES);
        this.writeHandler = new LoggerOperations.LoggerWriteAttributeHandler(writableAttributes);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition def : writableAttributes) {
            // Filter requires a special reader
            if (def.getName().equals(FILTER.getName())) {
                resourceRegistration.registerReadWriteAttribute(def, ReadFilterOperationStepHandler.INSTANCE, writeHandler);
            } else {
                resourceRegistration.registerReadWriteAttribute(def, null, writeHandler);
            }
        }
        resourceRegistration.registerReadOnlyAttribute(CATEGORY, null);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        super.registerOperations(registration);
        final ResourceDescriptionResolver resolver = getResourceDescriptionResolver();

        registration.registerOperationHandler(new SimpleOperationDefinition(CHANGE_LEVEL_OPERATION_NAME, resolver, CommonAttributes.LEVEL), LoggerOperations.CHANGE_LEVEL);
        registration.registerOperationHandler(new SimpleOperationDefinition(ADD_HANDLER_OPERATION_NAME, resolver, CommonAttributes.HANDLER_NAME), LoggerOperations.ADD_HANDLER);
        registration.registerOperationHandler(new SimpleOperationDefinition(REMOVE_HANDLER_OPERATION_NAME, resolver, CommonAttributes.HANDLER_NAME), LoggerOperations.REMOVE_HANDLER);

    }
}

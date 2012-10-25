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

import static org.jboss.as.logging.CommonAttributes.FILTER;
import static org.jboss.as.logging.CommonAttributes.FILTER_SPEC;
import static org.jboss.as.logging.CommonAttributes.HANDLERS;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.Logging.join;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.logging.LoggingOperations.ReadFilterOperationStepHandler;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class RootLoggerResourceDefinition extends SimpleResourceDefinition {
    public static final String ROOT_LOGGER_ADD_OPERATION_NAME = "set-root-logger";
    public static final String ROOT_LOGGER_REMOVE_OPERATION_NAME = "remove-root-logger";
    public static final String ROOT_LOGGER_CHANGE_LEVEL_OPERATION_NAME = "change-root-log-level";
    public static final String ROOT_LOGGER_ADD_HANDLER_OPERATION_NAME = "root-logger-assign-handler";
    public static final String ROOT_LOGGER_REMOVE_HANDLER_OPERATION_NAME = "root-logger-unassign-handler";
    static final PathElement ROOT_LOGGER_PATH = PathElement.pathElement(CommonAttributes.ROOT_LOGGER, CommonAttributes.ROOT_LOGGER_ATTRIBUTE_NAME);
    static final ResourceDescriptionResolver ROOT_RESOLVER = LoggingExtension.getResourceDescriptionResolver(CommonAttributes.ROOT_LOGGER);


    static final AttributeDefinition[] LEGACY_ATTRIBUTES = {
            FILTER,
    };

    static final AttributeDefinition[] ATTRIBUTES = {
            FILTER_SPEC,
            LEVEL,
            HANDLERS
    };

    static final SimpleOperationDefinition ROOT_LOGGER_REMOVE_OPERATION = new SimpleOperationDefinitionBuilder(ROOT_LOGGER_REMOVE_OPERATION_NAME, ROOT_RESOLVER)
            .setAttributeResolver(LoggingExtension.HANDLER_ATTRIBUTE_RESOLVER)
            .build();
    static final OperationDefinition ADD_ROOT_LOGGER_DEFINITION = new SimpleOperationDefinitionBuilder(ROOT_LOGGER_ADD_OPERATION_NAME, ROOT_RESOLVER)
            .setParameters(ATTRIBUTES)
            .setAttributeResolver(LoggingExtension.HANDLER_ATTRIBUTE_RESOLVER)
            .build();
    static final OperationDefinition CHANGE_LEVEL_OPERATION = new SimpleOperationDefinitionBuilder(ROOT_LOGGER_CHANGE_LEVEL_OPERATION_NAME, ROOT_RESOLVER)
            .setAttributeResolver(LoggingExtension.HANDLER_ATTRIBUTE_RESOLVER)
            .setParameters(CommonAttributes.LEVEL)
            .build();

    static final OperationDefinition ADD_HANDLER_OPERATION = new SimpleOperationDefinitionBuilder(ROOT_LOGGER_ADD_HANDLER_OPERATION_NAME, ROOT_RESOLVER)
            .setParameters(CommonAttributes.HANDLER_NAME)
            .build();

    static final OperationDefinition REMOVE_HANDLER_OPERATION = new SimpleOperationDefinitionBuilder(ROOT_LOGGER_REMOVE_HANDLER_OPERATION_NAME, ROOT_RESOLVER)
            .setParameters(CommonAttributes.HANDLER_NAME)
            .build();
    /**
     * A step handler to add a logger.
     */
    static final LoggerOperations.LoggerAddOperationStepHandler ADD_ROOT_LOGGER = new LoggerOperations.LoggerAddOperationStepHandler(ATTRIBUTES);
    /**
     * Write attribute handlers.
     */
    static final LoggerOperations.LoggerWriteAttributeHandler ROOT_LOGGER_WRITE_HANDLER = new LoggerOperations.LoggerWriteAttributeHandler(ATTRIBUTES);

    private final AttributeDefinition[] attributes;
    private final OperationStepHandler writeHandler;

    public RootLoggerResourceDefinition(final boolean includeLegacy) {
        super(ROOT_LOGGER_PATH,
                ROOT_RESOLVER,
                (includeLegacy ? new LoggerOperations.LoggerAddOperationStepHandler(join(ATTRIBUTES, LEGACY_ATTRIBUTES)) : new LoggerOperations.LoggerAddOperationStepHandler(ATTRIBUTES)),
                LoggerOperations.REMOVE_LOGGER);
        attributes = (includeLegacy ? join(ATTRIBUTES, LEGACY_ATTRIBUTES) : ATTRIBUTES);
        writeHandler = new LoggerOperations.LoggerWriteAttributeHandler(attributes);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition def : attributes) {
            if (def.getName().equals(FILTER.getName())) {
                resourceRegistration.registerReadWriteAttribute(def, ReadFilterOperationStepHandler.INSTANCE, writeHandler);
            } else {
                resourceRegistration.registerReadWriteAttribute(def, null, writeHandler);
            }
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        super.registerOperations(registration);

        registration.registerOperationHandler(ADD_ROOT_LOGGER_DEFINITION, RootLoggerResourceDefinition.ADD_ROOT_LOGGER);
        registration.registerOperationHandler(ROOT_LOGGER_REMOVE_OPERATION, LoggerOperations.REMOVE_LOGGER);
        registration.registerOperationHandler(CHANGE_LEVEL_OPERATION, LoggerOperations.CHANGE_LEVEL);
        registration.registerOperationHandler(ADD_HANDLER_OPERATION, LoggerOperations.ADD_HANDLER);
        registration.registerOperationHandler(REMOVE_HANDLER_OPERATION, LoggerOperations.REMOVE_HANDLER);
    }
}

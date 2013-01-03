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

import static org.jboss.as.logging.CommonAttributes.OVERFLOW_ACTION;
import static org.jboss.as.logging.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.logging.CommonAttributes.SUBHANDLERS;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.logmanager.handlers.AsyncHandler;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
class AsyncHandlerResourceDefinition extends AbstractHandlerDefinition {

    public static final String ADD_SUBHANDLER_OPERATION_NAME = "assign-subhandler";
    public static final String REMOVE_SUBHANDLER_OPERATION_NAME = "unassign-subhandler";
    static final PathElement ASYNC_HANDLER_PATH = PathElement.pathElement(CommonAttributes.ASYNC_HANDLER);

    static final SimpleOperationDefinition ADD_HANDLER = new SimpleOperationDefinitionBuilder(CommonAttributes.ADD_HANDLER_OPERATION_NAME, HANDLER_RESOLVER)
            .setParameters(CommonAttributes.HANDLER_NAME)
            .build();

    static final SimpleOperationDefinition REMOVE_HANDLER = new SimpleOperationDefinitionBuilder(CommonAttributes.REMOVE_HANDLER_OPERATION_NAME, HANDLER_RESOLVER)
            .setParameters(CommonAttributes.HANDLER_NAME)
            .build();

    static final SimpleOperationDefinition LEGACY_ADD_HANDLER = new SimpleOperationDefinitionBuilder(ADD_SUBHANDLER_OPERATION_NAME, HANDLER_RESOLVER)
            .setDeprecated(ModelVersion.create(1, 2, 0))
            .setParameters(CommonAttributes.HANDLER_NAME)
            .build();

    static final SimpleOperationDefinition LEGACY_REMOVE_HANDLER = new SimpleOperationDefinitionBuilder(REMOVE_SUBHANDLER_OPERATION_NAME, HANDLER_RESOLVER)
            .setDeprecated(ModelVersion.create(1, 2, 0))
            .setParameters(CommonAttributes.HANDLER_NAME)
            .build();

    static final AttributeDefinition[] ATTRIBUTES = Logging.join(DEFAULT_ATTRIBUTES, QUEUE_LENGTH, OVERFLOW_ACTION, SUBHANDLERS);


    public AsyncHandlerResourceDefinition(final boolean includeLegacyAttributes) {
        super(ASYNC_HANDLER_PATH, AsyncHandler.class, (includeLegacyAttributes ? Logging.join(ATTRIBUTES, LEGACY_ATTRIBUTES) : ATTRIBUTES), QUEUE_LENGTH);
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration registration) {
        super.registerOperations(registration);

        registration.registerOperationHandler(LEGACY_ADD_HANDLER, HandlerOperations.ADD_SUBHANDLER);
        registration.registerOperationHandler(LEGACY_REMOVE_HANDLER, HandlerOperations.REMOVE_SUBHANDLER);
        registration.registerOperationHandler(ADD_HANDLER, HandlerOperations.ADD_SUBHANDLER);
        registration.registerOperationHandler(REMOVE_HANDLER, HandlerOperations.REMOVE_SUBHANDLER);
    }
}

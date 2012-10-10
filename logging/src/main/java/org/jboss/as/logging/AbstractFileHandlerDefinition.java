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

import java.util.logging.Handler;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class AbstractFileHandlerDefinition extends AbstractHandlerDefinition {

    public static final String CHANGE_FILE_OPERATION_NAME = "change-file";
    private final ResolvePathHandler resolvePathHandler;

    protected AbstractFileHandlerDefinition(final PathElement path, final Class<? extends Handler> type,
                                            final ResolvePathHandler resolvePathHandler,
                                            final AttributeDefinition... attributes) {
        super(path, type, attributes);
        this.resolvePathHandler = resolvePathHandler;
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration registration) {
        super.registerOperations(registration);
        registration.registerOperationHandler(new SimpleOperationDefinition(CHANGE_FILE_OPERATION_NAME, getResourceDescriptionResolver(), CommonAttributes.FILE),
                HandlerOperations.CHANGE_FILE);
        if (resolvePathHandler != null)
            registration.registerOperationHandler(resolvePathHandler.getOperationDefinition(), resolvePathHandler);
    }
}

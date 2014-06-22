/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.DEFAULT_CLUSTERED_SFSB_CACHE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_CACHE;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.dmr.ModelNode;

/**
 * Validates that if {@link org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition#DEFAULT_CLUSTERED_SFSB_CACHE}
 * is set on a server that it matches
 * {@link org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition#DEFAULT_SFSB_CACHE}. Such a matched
 * value implies the configuration may be being used on both WildFly servers and legacy servers, which
 * still use the clustered cache ref.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
class ValidateClusteredCacheRefHandler implements OperationStepHandler {
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (context.getProcessType().isServer()) {
            ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
            if (model.hasDefined(DEFAULT_CLUSTERED_SFSB_CACHE.getName())
                && !model.get(DEFAULT_CLUSTERED_SFSB_CACHE.getName()).equals(model.get(DEFAULT_SFSB_CACHE.getName()))) {
                throw EjbLogger.ROOT_LOGGER.inconsistentAttributeNotSupported(DEFAULT_CLUSTERED_SFSB_CACHE.getName(), DEFAULT_SFSB_CACHE.getName());
            }
        }
        context.stepCompleted();
    }
}

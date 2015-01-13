/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.mod_cluster;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * {@code OperationHandler} responsible for defining the accesslog entry.
 *
 * @author Jean-Frederic Clere
 */
class ModClusterAddSSL extends AbstractAddStepHandler {

    static final ModClusterAddSSL INSTANCE = new ModClusterAddSSL();

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        // Our Stage.RUNTIME handling only sets context.reloadRequired();
        // We only need to do that if ModClusterSubsystemAdd isn't running in the
        // same overall operation. So check if they are
        return !ModClusterSubsystemAdd.isActiveInContext(context) && super.requiresRuntime(context);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        // need to set reload-required on the server
        context.reloadRequired();
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, Resource resource) {
        context.revertReloadRequired();
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr : ModClusterSSLResourceDefinition.ATTRIBUTES) {
            attr.validateAndSet(operation, model);
        }
    }
}

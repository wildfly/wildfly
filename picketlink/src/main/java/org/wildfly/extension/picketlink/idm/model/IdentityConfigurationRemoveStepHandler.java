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

package org.wildfly.extension.picketlink.idm.model;

import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

import java.util.Set;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.picketlink.common.model.ModelElement;

/**
 * @author Pedro Silva
 */
public class IdentityConfigurationRemoveStepHandler extends AbstractRemoveStepHandler {

    static final IdentityConfigurationRemoveStepHandler INSTANCE = new IdentityConfigurationRemoveStepHandler();

    private IdentityConfigurationRemoveStepHandler() {
    }

    @Override
    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        super.performRemove(context, operation, model);
        PathAddress partitionManagerAddress = context.getCurrentAddress().getParent();
        Resource partitionManagerResource = context.readResourceFromRoot(partitionManagerAddress);
        checkIfLastConfiguration(partitionManagerResource);
    }

    private void checkIfLastConfiguration(Resource partitionManagerResource) throws OperationFailedException {
        Set<Resource.ResourceEntry> configurations = partitionManagerResource.getChildren(ModelElement.IDENTITY_CONFIGURATION.getName());

        if (configurations.isEmpty() || configurations.size() == 1) {
            throw ROOT_LOGGER.idmNoIdentityConfigurationProvided();
        }
    }

}

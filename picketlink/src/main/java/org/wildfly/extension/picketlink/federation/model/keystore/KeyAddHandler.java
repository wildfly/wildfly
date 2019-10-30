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

package org.wildfly.extension.picketlink.federation.model.keystore;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.picketlink.federation.service.KeyService;
import org.wildfly.extension.picketlink.federation.service.KeyStoreProviderService;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class KeyAddHandler extends AbstractAddStepHandler {

    static final KeyAddHandler INSTANCE = new KeyAddHandler();

    static void launchServices(OperationContext context, PathAddress pathAddress, ModelNode model) throws OperationFailedException {
        String federationAlias = pathAddress.subAddress(0, pathAddress.size() - 2).getLastElement().getValue();
        String keyName = pathAddress.getLastElement().getValue();
        String host = KeyResourceDefinition.HOST.resolveModelAttribute(context, model).asString();
        KeyService service = new KeyService(keyName, host);
        context.getServiceTarget().addService(KeyService.createServiceName(federationAlias, keyName), service)
                .addDependency(KeyStoreProviderService.createServiceName(federationAlias), KeyStoreProviderService.class, service.getKeyStoreProviderService())
                .setInitialMode(ServiceController.Mode.PASSIVE)
                .install();
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (SimpleAttributeDefinition attribute : KeyResourceDefinition.INSTANCE.getAttributes()) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        launchServices(context, PathAddress.pathAddress(operation.get(ADDRESS)), model);
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, Resource resource) {
        try {
            KeyRemoveHandler.INSTANCE.performRuntime(context, operation, resource.getModel());
        } catch (OperationFailedException ignore) {
        }
    }
}

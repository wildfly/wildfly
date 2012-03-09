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

package org.jboss.as.modcluster;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.as.web.WebServer;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;

import java.util.List;
import java.util.Set;

/**
 * The managed subsystem add update.
 *
 * @author Jean-Frederic Clere
 * @author Tomaz Cerar
 */
class ModClusterSubsystemAdd extends AbstractAddStepHandler {

    static final ModClusterSubsystemAdd INSTANCE = new ModClusterSubsystemAdd();


    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
                                  ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        final ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        final ModelNode resolvedModel = resolve(context, fullModel);
        final ModelNode config = resolvedModel.get(ModClusterExtension.CONFIGURATION_PATH.getKeyValuePair());
        // Add mod_cluster service
        final ModClusterService service = new ModClusterService(config);
        final ServiceBuilder<ModCluster> serviceBuilder = context.getServiceTarget().addService(ModClusterService.NAME, service)
                .addDependency(WebSubsystemServices.JBOSS_WEB, WebServer.class, service.getWebServer())
                .addDependency(SocketBindingManager.SOCKET_BINDING_MANAGER, SocketBindingManager.class, service.getBindingManager())
                .addListener(verificationHandler)
                .setInitialMode(Mode.ACTIVE);
        final ModelNode bindingRefNode = config.get(ModClusterConfigResourceDefinition.ADVERTISE_SOCKET.getName());
        final String bindingRef = bindingRefNode.isDefined() ? bindingRefNode.asString() : null;
        if (bindingRef != null) {
            serviceBuilder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(bindingRef), SocketBinding.class, service.getBinding());
        }
        newControllers.add(serviceBuilder.install());
    }


    private ModelNode resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        ImmutableManagementResourceRegistration resource = context.getResourceRegistration();
        return resolveRecursive(context, context.readResource(PathAddress.EMPTY_ADDRESS), resource, model);
    }

    private ModelNode resolveRecursive(OperationContext context, final Resource resource, ImmutableManagementResourceRegistration registration, ModelNode model) throws OperationFailedException {
        ModelNode resolved = new ModelNode();
        Set<String> attributeNames = registration.getAttributeNames(PathAddress.EMPTY_ADDRESS);
        for (String name : attributeNames) {
            AttributeDefinition def = registration.getAttributeAccess(PathAddress.EMPTY_ADDRESS, name).getAttributeDefinition();
            resolved.get(name).set(def.resolveModelAttribute(context, model));
        }
        for (PathElement element : registration.getChildAddresses(PathAddress.EMPTY_ADDRESS)) {
            if (element.isMultiTarget()) {
                final String childType = element.getKey();
                for (final Resource.ResourceEntry entry : resource.getChildren(childType)) {
                    final ImmutableManagementResourceRegistration childRegistration = registration.getSubModel(PathAddress.pathAddress(PathElement.pathElement(childType, entry.getName())));
                    ModelNode unResolvedSubModel = model.get(entry.getPathElement().getKeyValuePair());
                    resolved.get(entry.getPathElement().getKeyValuePair()).set(resolveRecursive(context, entry, childRegistration, unResolvedSubModel));
                }
            } else {
                final Resource child = resource.getChild(element);
                final ImmutableManagementResourceRegistration childRegistration = registration.getSubModel(PathAddress.pathAddress(element));
                ModelNode unResolvedSubModel = model.get(element.getKeyValuePair());
                resolved.get(element.getKeyValuePair()).set(resolveRecursive(context, child, childRegistration, unResolvedSubModel));
            }
        }
        return resolved;
    }


    @Override
    protected void populateModel(ModelNode operation, ModelNode model) {

    }

}

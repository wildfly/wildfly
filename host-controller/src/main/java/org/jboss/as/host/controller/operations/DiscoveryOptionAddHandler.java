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
package org.jboss.as.host.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.host.controller.discovery.DiscoveryOption;
import org.jboss.as.host.controller.discovery.DiscoveryOptionResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceController;

/**
 * Handler for the discovery option resource's add operation.
 *
 * @author Farah Juma
 */
public class DiscoveryOptionAddHandler extends AbstractAddStepHandler {

    public static final String OPERATION_NAME = ADD;
    private final LocalHostControllerInfoImpl hostControllerInfo;

    /**
     * Create the DiscoveryOptionAddHandler.
     *
     * @param hostControllerInfo the host controller info
     */
    public DiscoveryOptionAddHandler(final LocalHostControllerInfoImpl hostControllerInfo) {
        this.hostControllerInfo = hostControllerInfo;
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                  final ServiceVerificationHandler verificationHandler,
                                  final List<ServiceController<?>> newControllers) throws OperationFailedException {
        if (context.isBooting()) {
            populateHostControllerInfo(hostControllerInfo, context, operation, model);
        } else {
            context.reloadRequired();
        }
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (final AttributeDefinition attribute : DiscoveryOptionResourceDefinition.DISCOVERY_ATTRIBUTES) {
            attribute.validateAndSet(operation, model);
        }
    }

    protected void populateHostControllerInfo(LocalHostControllerInfoImpl hostControllerInfo, OperationContext context,
            ModelNode operation, ModelNode model) throws OperationFailedException {
        ModelNode codeNode = DiscoveryOptionResourceDefinition.CODE.resolveModelAttribute(context, model);
        String discoveryOptionClassName = codeNode.isDefined() ? codeNode.asString() : null;

        ModelNode moduleNode = DiscoveryOptionResourceDefinition.MODULE.resolveModelAttribute(context, model);
        String moduleName = moduleNode.isDefined() ? moduleNode.asString() : null;

        final Map<String, ModelNode> discoveryOptionProperties = new HashMap<String, ModelNode>();
        if (operation.hasDefined(DiscoveryOptionResourceDefinition.PROPERTIES.getName())) {
            for (Map.Entry<String, String> discoveryOptionProperty : DiscoveryOptionResourceDefinition.PROPERTIES.unwrap(context, model).entrySet()) {
                discoveryOptionProperties.put(discoveryOptionProperty.getKey(), new ModelNode(discoveryOptionProperty.getValue()));
            }
        }

        try {
            ModuleIdentifier moduleID = ModuleIdentifier.fromString(moduleName);
            final Class<? extends DiscoveryOption> discoveryOptionClass = Module.loadClassFromCallerModuleLoader(moduleID, discoveryOptionClassName)
                    .asSubclass(DiscoveryOption.class);
            final Constructor<? extends DiscoveryOption> constructor = discoveryOptionClass.getConstructor(new Class[]{Map.class});
            final DiscoveryOption discoveryOption = constructor.newInstance(discoveryOptionProperties);
            hostControllerInfo.addRemoteDomainControllerDiscoveryOption(discoveryOption);
        } catch (Exception e) {
            throw MESSAGES.cannotInstantiateDiscoveryOptionClass(discoveryOptionClassName, e.getLocalizedMessage());
        }
    }
}

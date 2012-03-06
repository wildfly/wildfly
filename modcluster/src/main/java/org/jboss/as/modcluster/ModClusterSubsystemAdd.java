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

import static org.jboss.as.modcluster.ModClusterLogger.ROOT_LOGGER;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.as.web.WebServer;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modcluster.config.impl.ModClusterConfig;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.omg.CosCollection.Command;

/**
 * The managed subsystem add update.
 *
 * @author Jean-Frederic Clere
 */
class ModClusterSubsystemAdd extends AbstractAddStepHandler implements DescriptionProvider {

    static final ModClusterSubsystemAdd INSTANCE = new ModClusterSubsystemAdd();
    static final PathElement SSLPath = PathElement.pathElement(CommonAttributes.SSL, CommonAttributes.CONFIGURATION);
    static final PathElement confPath = PathElement.pathElement(CommonAttributes.MOD_CLUSTER_CONFIG, CommonAttributes.CONFIGURATION);

    @Override
    protected void populateModel(final ModelNode operation, final Resource resource) throws OperationFailedException {
         if (operation.hasDefined(CommonAttributes.MOD_CLUSTER_CONFIG)) {
            ModelNode configuration;
            if (operation.get(CommonAttributes.MOD_CLUSTER_CONFIG).hasDefined(CommonAttributes.CONFIGURATION)) {
                configuration = operation.get(CommonAttributes.MOD_CLUSTER_CONFIG).get(CommonAttributes.CONFIGURATION);
            }else {
                configuration = operation.get(CommonAttributes.MOD_CLUSTER_CONFIG);
            }
            resource.registerChild(confPath, Resource.Factory.create());
            final Resource conf = resource.getChild(confPath);
            final ModelNode confModel = conf.getModel();
            for(final String attribute : configuration.keys()) {
                if (attribute.equals(CommonAttributes.SSL)) {
                    conf.registerChild(SSLPath, Resource.Factory.create());
                    final Resource ssl = conf.getChild(SSLPath);
                    ModelNode sslnode;
                    if (configuration.get(attribute).hasDefined(CommonAttributes.CONFIGURATION)) {
                        sslnode = configuration.get(attribute).get(CommonAttributes.CONFIGURATION);
                    } else {
                        sslnode = configuration.get(attribute);
                    }
                    final ModelNode sslModel = ssl.getModel();
                    for (AttributeDefinition sslAttr : ModClusterSSLResourceDefinition.ATTRIBUTES) {
                        sslAttr.validateAndSet(sslnode, sslModel);
                    }
                }
                else if (attribute.equals(CommonAttributes.DYNAMIC_LOAD_PROVIDER) || attribute.equals(CommonAttributes.SIMPLE_LOAD_PROVIDER)) {
                    // TODO AS7-4050 properly handle these
                    confModel.get(attribute).set(configuration.get(attribute));
                } else {
                    AttributeDefinition ad = ModClusterConfigResourceDefinition.ATTRIBUTES_BY_NAME.get(attribute);
                    if (ad != null) {
                        ad.validateAndSet(configuration, confModel);
                    } // else ignore unknown params
                }
            }
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
                                  ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        final ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        // Use ModClusterExtension.configurationPath here so if we change the PathElement components,
        // we don't have to change this code
        final ModelNode unresolvedConfig = fullModel.get(ModClusterExtension.configurationPath.getKey(), ModClusterExtension.configurationPath.getValue());
        final ModelNode resolvedConfig = resolveConfig(context, unresolvedConfig);

        // Add mod_cluster service
        final ModClusterService service = new ModClusterService(resolvedConfig);
        final ServiceBuilder<ModCluster> serviceBuilder = context.getServiceTarget().addService(ModClusterService.NAME, service)
                .addDependency(WebSubsystemServices.JBOSS_WEB, WebServer.class, service.getWebServer())
                .addDependency(SocketBindingManager.SOCKET_BINDING_MANAGER, SocketBindingManager.class, service.getBindingManager())
                .addListener(verificationHandler)
                .setInitialMode(Mode.ACTIVE);
        final ModelNode bindingRefNode = resolvedConfig.get(ModClusterConfigResourceDefinition.ADVERTISE_SOCKET.getName());
        final String bindingRef = bindingRefNode.isDefined() ? bindingRefNode.asString() : null;
         if (bindingRef != null) {
            serviceBuilder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(bindingRef), SocketBinding.class, service.getBinding());
         }

        newControllers.add(serviceBuilder.install());
    }

    /**
     * Creates a copy of unresolvedConfig with all expressions resolved and all undefined attributes that
     * have a default value set to the default.
     * @param context the operation context
     * @param unresolvedConfig the raw configuration model
     * @return the resolved configuration model
     * @throws OperationFailedException if there is a problem resolving an attribute
     */
    private ModelNode resolveConfig(OperationContext context, ModelNode unresolvedConfig) throws OperationFailedException {

        final ModelNode resolved = new ModelNode();

        // First the simple core attributes
        for (AttributeDefinition coreAttr : ModClusterConfigResourceDefinition.ATTRIBUTES) {
            resolved.get(coreAttr.getName()).set(coreAttr.resolveModelAttribute(context, unresolvedConfig));
        }
        // Next SSL
        // Use ModClusterExtension.sslConfigurationPath here so if we change the PathElement components,
        // we don't have to change this code
        final ModelNode unresolvedSSL = unresolvedConfig.get(ModClusterExtension.sslConfigurationPath.getKey(),
                                                             ModClusterExtension.sslConfigurationPath.getValue());
        if (unresolvedSSL.isDefined()) {
            final ModelNode resolvedSSL = resolved.get(ModClusterExtension.sslConfigurationPath.getKey(),
                                                       ModClusterExtension.sslConfigurationPath.getValue());
            for (AttributeDefinition sslAttr : ModClusterConfigResourceDefinition.ATTRIBUTES) {
                resolvedSSL.get(sslAttr.getName()).set(sslAttr.resolveModelAttribute(context, unresolvedSSL));
            }
        }

        // Finally the load-provider stuff
        // TODO AS7-4050 properly handle these
        for (Property property : unresolvedConfig.asPropertyList()) {
            String key = property.getName();
            if (!ModClusterConfigResourceDefinition.ATTRIBUTES_BY_NAME.containsKey(key) &&
                    !key.equals(ModClusterExtension.sslConfigurationPath.getKey())) {
                resolved.get(key).set(context.resolveExpressions(property.getValue()));
            }
        }

        return resolved;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ModClusterSubsystemDescriptions.getSubsystemAddDescription(locale);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) {
        // not called because we override the variant that takes a Resource param; we just implement it because it is abstract
    }
}

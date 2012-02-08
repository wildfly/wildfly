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

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.as.web.WebServer;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;

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
    protected void populateModel(final ModelNode operation, final Resource resource) {
         if (operation.hasDefined(CommonAttributes.MOD_CLUSTER_CONFIG)) {
            ModelNode configuration;
            if (operation.get(CommonAttributes.MOD_CLUSTER_CONFIG).hasDefined(CommonAttributes.CONFIGURATION)) {
                configuration = operation.get(CommonAttributes.MOD_CLUSTER_CONFIG).get(CommonAttributes.CONFIGURATION);
            }else {
                configuration = operation.get(CommonAttributes.MOD_CLUSTER_CONFIG);
            }
            resource.registerChild(confPath, Resource.Factory.create());
            final Resource conf = resource.getChild(confPath);
            for(final String attribute : configuration.keys()) {
                if (attribute.equals(CommonAttributes.SSL)) {
                    conf.registerChild(SSLPath, Resource.Factory.create());
                    final Resource ssl = conf.getChild(SSLPath);
                    ModelNode sslnode;
                    if (configuration.get(attribute).hasDefined(CommonAttributes.CONFIGURATION))
                        sslnode = configuration.get(attribute).get(CommonAttributes.CONFIGURATION);
                    else
                        sslnode = configuration.get(attribute);
                    populateConf(ssl.getModel(), sslnode);
                }
                else
                    conf.getModel().get(attribute).set(configuration.get(attribute));
            }
        }
    }

    static void populateConf(final ModelNode subModel, final ModelNode operation) {
        for(final String attribute : operation.keys()) {
            if(operation.hasDefined(attribute)) {
                subModel.get(attribute).set(operation.get(attribute));
            }
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        String bindingRef = null;
        ModelNode node = operation.get(CommonAttributes.MOD_CLUSTER_CONFIG);
        List<String> outboundSocketBindings = new LinkedList<String>();
        if (operation.hasDefined(CommonAttributes.MOD_CLUSTER_CONFIG)) {
            if (operation.get(CommonAttributes.MOD_CLUSTER_CONFIG).hasDefined(CommonAttributes.CONFIGURATION))
                node = operation.get(CommonAttributes.MOD_CLUSTER_CONFIG).get(CommonAttributes.CONFIGURATION);
            if (node.hasDefined(CommonAttributes.ADVERTISE_SOCKET)) {
                bindingRef = node.get(CommonAttributes.ADVERTISE_SOCKET).asString();
            }
            if (node.hasDefined(CommonAttributes.PROXY_LIST)) {
                for (ModelNode proxy: node.get(CommonAttributes.PROXY_LIST).asList()) {
                    outboundSocketBindings.add(proxy.asString());
                }
            }
        }
        try {
            //Get the unmasked password
            // Add mod_cluster service
            final ModelNode resolved = context.resolveExpressions(node.clone());
            final ModClusterService service = new ModClusterService(unmaskPassword(context, model), resolved);
            final ServiceBuilder<ModCluster> serviceBuilder = context.getServiceTarget().addService(ModClusterService.NAME, service)
                    // .addListener(new ResultHandler.ServiceStartListener(resultHandler))
                    .addDependency(WebSubsystemServices.JBOSS_WEB, WebServer.class, service.getWebServer())
                    .addDependency(SocketBindingManager.SOCKET_BINDING_MANAGER, SocketBindingManager.class, service.getBindingManager())
                    .addListener(verificationHandler)
                    .setInitialMode(Mode.ACTIVE);
            if (bindingRef != null) {
                serviceBuilder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(bindingRef), SocketBinding.class, service.getBinding());
            }
            for (String outboundSocketBinding: outboundSocketBindings) {
                serviceBuilder.addDependency(OutboundSocketBinding.OUTBOUND_SOCKET_BINDING_BASE_SERVICE_NAME.append(outboundSocketBinding), OutboundSocketBinding.class, service.addOutboundSocketBinding());
            }
            newControllers.add(serviceBuilder.install());
        } catch (Throwable t) {
            ROOT_LOGGER.debugf("Error: %s", t);
            throw new OperationFailedException(new ModelNode().set(t.getLocalizedMessage()));
        }
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ModClusterSubsystemDescriptions.getSubsystemAddDescription(locale);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) {
        // TODO Auto-generated method stub

    }

    private String unmaskPassword(OperationContext context, ModelNode model) throws OperationFailedException {
        if (!model.hasDefined(CommonAttributes.SSL)) {
            return null;
        }
        if (!model.get(CommonAttributes.SSL).hasDefined(CommonAttributes.PASSWORD)) {
            return null;
        }
        return context.resolveExpressions(model.get(CommonAttributes.SSL, CommonAttributes.PASSWORD)).toString();
    }

}

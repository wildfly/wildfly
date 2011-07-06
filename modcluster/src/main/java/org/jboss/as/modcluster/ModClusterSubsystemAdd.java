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

import java.util.List;
import java.util.Locale;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.as.web.WebServer;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
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
    private static final Logger log = Logger.getLogger("org.jboss.as.modcluster");


    protected void populateModel(ModelNode operation, ModelNode model) {
        model.set(operation.get(CommonAttributes.MOD_CLUSTER_CONFIG));
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        String bindingRef = null;
        if (operation.hasDefined(CommonAttributes.MOD_CLUSTER_CONFIG)) {
            final ModelNode node = operation.get(CommonAttributes.MOD_CLUSTER_CONFIG);
            if (node.hasDefined(CommonAttributes.ADVERTISE_SOCKET)) {
                bindingRef = node.get(CommonAttributes.ADVERTISE_SOCKET).asString();
            }
        }
        try {
            // Add mod_cluster service
            final ModClusterService service = new ModClusterService(operation.get(CommonAttributes.MOD_CLUSTER_CONFIG).clone());
            final ServiceBuilder<ModCluster> serviceBuilder = context.getServiceTarget().addService(ModClusterService.NAME, service)
                    // .addListener(new ResultHandler.ServiceStartListener(resultHandler))
                    .addDependency(WebSubsystemServices.JBOSS_WEB, WebServer.class, service.getWebServer())
                    .addDependency(SocketBindingManager.SOCKET_BINDING_MANAGER, SocketBindingManager.class, service.getBindingManager())
                    .addListener(verificationHandler)
                    .setInitialMode(Mode.ACTIVE);
             if (bindingRef != null)
                serviceBuilder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(bindingRef), SocketBinding.class, service.getBinding());

            newControllers.add(serviceBuilder.install());
        } catch (Throwable t) {
            log.error("Error: " + t);
            throw new OperationFailedException(new ModelNode().set(t.getLocalizedMessage()));
        }
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ModClusterSubsystemDescriptions.getSubsystemAddDescription(locale);
    }

}

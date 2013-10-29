/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.remoting;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.remoting3.Endpoint;
import org.wildfly.extension.io.IOServices;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.xnio.OptionMap;
import org.xnio.XnioWorker;

/**
 * Add operation handler for the remoting subsystem.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
class RemotingSubsystemAdd extends AbstractAddStepHandler {

    static final RemotingSubsystemAdd DOMAIN = new RemotingSubsystemAdd(false);
    static final RemotingSubsystemAdd SERVER = new RemotingSubsystemAdd(true);

    private final boolean server;

    private RemotingSubsystemAdd(final boolean server) {
        this.server = server;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (final AttributeDefinition attribute : RemotingSubsystemRootResource.ATTRIBUTES) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        launchServices(context, model, verificationHandler, newControllers);
    }

    void launchServices(final OperationContext context, final ModelNode model, final ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        if (areWorkerAttributesSet(context, model)) {
            throw RemotingMessages.MESSAGES.workerConfigurationIgnored();
        }

        ModelNode endpointModel = context.readResource(PathAddress.pathAddress(RemotingEndpointResource.ENDPOINT_PATH)).getModel();
        String workerName = RemotingEndpointResource.WORKER.resolveModelAttribute(context, endpointModel).asString();

        final OptionMap map = EndpointConfigFactory.populate(context, endpointModel);

        final ServiceTarget serviceTarget = context.getServiceTarget();

        // create endpoint
        final String nodeName = WildFlySecurityManager.getPropertyPrivileged(RemotingExtension.NODE_NAME_PROPERTY, null);
        final EndpointService endpointService = new EndpointService(nodeName, EndpointService.EndpointType.SUBSYSTEM, map);

        // In case of a managed server the subsystem endpoint might already be installed {@see DomainServerCommunicationServices}
        if (context.getProcessType() == ProcessType.DOMAIN_SERVER) {
            final ServiceController<?> controller = context.getServiceRegistry(false).getService(RemotingServices.SUBSYSTEM_ENDPOINT);
            if (controller != null) {
                // if installed, just skip the rest
                return;
            }
        }
        final ServiceBuilder<Endpoint> builder = serviceTarget.addService(RemotingServices.SUBSYSTEM_ENDPOINT, endpointService)
                .addDependency(IOServices.WORKER.append(workerName), XnioWorker.class, endpointService.getWorker());

        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }

        ServiceController<?> controller = builder.install();
        if (newControllers != null) {
            newControllers.add(controller);
        }
    }

    private boolean areWorkerAttributesSet(final OperationContext context, final ModelNode model) throws OperationFailedException {
        for (final AttributeDefinition attribute : RemotingSubsystemRootResource.ATTRIBUTES) {
            ModelNode value = attribute.resolveModelAttribute(context,model);
            if (value.isDefined() && !value.equals(attribute.getDefaultValue())) {
                return true;
            }
        }
        return false;
    }
}

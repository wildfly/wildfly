/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.osgi.service;

import static org.jboss.as.osgi.OSGiConstants.SERVICE_BASE_NAME;
import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.server.Services.JBOSS_SERVER_CONTROLLER;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.osgi.framework.spi.IntegrationService.BootstrapPhase;
import org.jboss.osgi.framework.spi.ServiceTracker;

/**
 * A service that tracks initial deployments.
 *
 * @author thomas.diesler@jboss.com
 * @since 12-Apr-2012
 */
public class InitialDeploymentTracker extends ServiceTracker<Object> {

    private static final ServiceName INITIAL_DEPLOYMENTS = SERVICE_BASE_NAME.append("initial", "deployments");

    public static final ServiceName INITIAL_DEPLOYMENTS_COMPLETE = BootstrapPhase.serviceName(INITIAL_DEPLOYMENTS, BootstrapPhase.COMPLETE);

    private final Set<ServiceName> expectedServices = Collections.synchronizedSet(new HashSet<ServiceName>());
    private final ServiceTarget serviceTarget;
    private final Set<String> deploymentNames;

    private ServiceTarget listenerTarget;

    public InitialDeploymentTracker(OperationContext context, ServiceVerificationHandler verificationHandler) {

        serviceTarget = context.getServiceTarget();
        deploymentNames = getDeploymentNames(context);

        // Track the persistent REGISTER services
        for (String name : deploymentNames) {
            expectedServices.add(Services.deploymentUnitName(name, Phase.REGISTER));
        }

        // Register this tracker with the server controller
        if (expectedServices.isEmpty() == false) {
            ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
            listenerTarget = serviceRegistry.getService(JBOSS_SERVER_CONTROLLER).getServiceContainer();
            listenerTarget.addListener(Inheritance.ALL, this);
        }

        // Check the tracker for completeness
        checkAndComplete();
    }

    public ServiceTarget getServiceTarget() {
        return serviceTarget;
    }

    @Override
    protected boolean trackService(ServiceController<? extends Object> controller) {
        ServiceName serviceName = controller.getName();
        return expectedServices.contains(serviceName);
    }

    @Override
    protected void serviceListenerAdded(ServiceController<? extends Object> controller) {
        ServiceName serviceName = controller.getName();
        LOGGER.debugf("Track service: %s", serviceName);
        expectedServices.remove(serviceName);
    }

    @Override
    protected boolean allServicesAdded(Set<ServiceName> trackedServices) {
        return expectedServices.isEmpty();
    }

    @Override
    protected void serviceStarted(ServiceController<? extends Object> controller) {
        ServiceName serviceName = controller.getName();
        LOGGER.debugf("ServiceStarted: %s", serviceName);
    }

    @Override
    protected void serviceStartFailed(ServiceController<? extends Object> controller) {
        ServiceName serviceName = controller.getName();
        LOGGER.warnf("ServiceStartFailed: %s", serviceName);
    }

    @Override
    protected void complete() {
        LOGGER.debugf("Initial deployments complete");
        if (listenerTarget != null) {
            listenerTarget.removeListener(this);
        }
        addPhaseCompleteService(serviceTarget, INITIAL_DEPLOYMENTS_COMPLETE);
    }

    public boolean hasDeploymentName(String depname) {
        return deploymentNames.contains(depname);
    }

    private Set<String> getDeploymentNames(OperationContext context) {
        final Set<String> result = new HashSet<String>();
        final ModelNode model = Resource.Tools.readModel(context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, true));
        final ModelNode depmodel = model.get(ModelDescriptionConstants.DEPLOYMENT);
        if (depmodel.isDefined()) {
            final List<ModelNode> deploymentNodes = depmodel.asList();
            for (ModelNode node : deploymentNodes) {
                Property property = node.asProperty();
                ModelNode enabled = property.getValue().get(ModelDescriptionConstants.ENABLED);
                if (enabled.isDefined() && enabled.asBoolean()) {
                    result.add(property.getName());
                }
            }
            LOGGER.debugf("Expecting initial deployments: %s", result);
        }
        return result;
    }

    private ServiceController<Object> addPhaseCompleteService(ServiceTarget serviceTarget, ServiceName serviceName) {
        LOGGER.debugf("addPhaseCompleteService: %s", serviceName);
        return serviceTarget.addService(serviceName, new ValueService<Object>(new ImmediateValue<Object>(new Object()))).install();
    }

}

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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.jboss.osgi.framework.spi.ServiceTracker;
import org.jboss.osgi.framework.spi.IntegrationService.BootstrapPhase;

/**
 * A service that tracks initial deployments.
 *
 * @author thomas.diesler@jboss.com
 * @since 12-Apr-2012
 */
public class InitialDeploymentTracker extends ServiceTracker<Object> {

    private static final ServiceName INITIAL_DEPLOYMENTS = SERVICE_BASE_NAME.append("initial", "deployments");

    public static final ServiceName REGISTER_PHASE_SERVICES_COMPLETE = INITIAL_DEPLOYMENTS.append("REGISTERED");
    public static final ServiceName INITIAL_DEPLOYMENTS_COMPLETE = BootstrapPhase.serviceName(INITIAL_DEPLOYMENTS, BootstrapPhase.COMPLETE);

    private final Map<String, Set<ServiceName>> deploymentPhaseServices = new HashMap<String, Set<ServiceName>>();
    private final Map<String, AtomicInteger> deploymentPhaseCounts = new HashMap<String, AtomicInteger>();
    private final ServiceTarget serviceTarget;
    private final Set<String> deploymentNames;

    private ServiceTarget listenerTarget;

    public InitialDeploymentTracker(OperationContext context, ServiceVerificationHandler verificationHandler) {

        serviceTarget = context.getServiceTarget();
        deploymentNames = getDeploymentNames(context);

        // Get the expected phase service names
        for (Phase phase : Arrays.asList(Phase.REGISTER, Phase.INSTALL)) {
            HashSet<ServiceName> serviceNames = new HashSet<ServiceName>();
            deploymentPhaseServices.put(phase.toString(), serviceNames);
            deploymentPhaseCounts.put(phase.toString(), new AtomicInteger(deploymentNames.size()));
            for (String deploymentName : deploymentNames) {
                ServiceName serviceName = Services.deploymentUnitName(deploymentName);
                serviceNames.add(serviceName.append(phase.toString()));
            }
        }

        // Register this tracker with the server controller
        if (deploymentNames.isEmpty() == false) {
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
        // [TODO] currently we track all persistet deployments.
        // If one fails it would mean that the OSGi framwork does not bootstrap
        ServiceName serviceName = controller.getName();
        String phaseName = serviceName.getSimpleName();
        Set<ServiceName> serviceNames = deploymentPhaseServices.get(phaseName);
        boolean trackService = serviceNames != null && serviceNames.contains(serviceName);
        if (trackService) {
            LOGGER.debugf("Track service: %s", serviceName);
        }
        return trackService;
    }

    @Override
    protected boolean allServicesAdded(Set<ServiceName> trackedServices) {
        return deploymentPhaseServices.size() * deploymentNames.size() == trackedServices.size();
    }

    @Override
    protected void serviceStarted(ServiceController<? extends Object> controller) {
        ServiceName serviceName = controller.getName();
        String phaseName = serviceName.getSimpleName();
        AtomicInteger phaseCount = deploymentPhaseCounts.get(phaseName);
        if (phaseCount != null) {
            int remainingCount = phaseCount.decrementAndGet();
            LOGGER.debugf("ServiceStarted: %s [%d]", serviceName, remainingCount);
            if (Phase.REGISTER.toString().equals(phaseName) && remainingCount == 0) {
                addPhaseCompleteService(serviceTarget, REGISTER_PHASE_SERVICES_COMPLETE);
            }
        }
    }

    @Override
    protected void complete() {
        LOGGER.tracef("Initial deployments complete");
        if (listenerTarget != null) {
            listenerTarget.removeListener(this);
        }
        addPhaseCompleteService(serviceTarget, INITIAL_DEPLOYMENTS_COMPLETE);
    }

    public boolean isComplete() {
        AtomicInteger installCount = deploymentPhaseCounts.get(Phase.INSTALL.toString());
        return installCount.get() == 0;
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
            LOGGER.tracef("Expecting initial deployments: %s", result);
        }
        return result;
    }

    private ServiceController<Object> addPhaseCompleteService(ServiceTarget serviceTarget, ServiceName serviceName) {
        LOGGER.debugf("addPhaseCompleteService: %s", serviceName);
        return serviceTarget.addService(serviceName, new ValueService<Object>(new ImmediateValue<Object>(new Object()))).install();
    }

}
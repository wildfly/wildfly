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

package org.jboss.as.clustering.singleton.deployment.subsystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.clustering.singleton.SingletonElectionPolicy;
import org.jboss.as.clustering.singleton.deployment.SingletonDeploymentUnitPhaseServiceBuilder;
import org.jboss.as.clustering.singleton.election.NamePreference;
import org.jboss.as.clustering.singleton.election.Preference;
import org.jboss.as.clustering.singleton.election.PreferredSingletonElectionPolicy;
import org.jboss.as.clustering.singleton.election.SimpleSingletonElectionPolicy;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Paul Ferraro
 */
public class DeploymentPolicyAddHandler extends AbstractAddStepHandler {
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attribute: DeploymentPolicyResourceDefinition.ATTRIBUTES) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        newControllers.addAll(installRuntimeServices(context, operation, model, verificationHandler));
    }

    static Collection<ServiceController<?>> installRuntimeServices(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        String name = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();
        final ServiceTarget target = context.getServiceTarget();

        final String container = DeploymentPolicyResourceDefinition.CACHE_CONTAINER.resolveModelAttribute(context, model).asString();

        SingletonElectionPolicy electionPolicy = new SimpleSingletonElectionPolicy();
        ModelNode preferredNodes = DeploymentPolicyResourceDefinition.PREFERRED_NODES.resolveModelAttribute(context, model);
        if (preferredNodes.isDefined()) {
            final List<ModelNode> nodes = preferredNodes.asList();
            final List<Preference> preferences = new ArrayList<Preference>(nodes.size());
            for (ModelNode node: nodes) {
                preferences.add(new NamePreference(node.asString()));
            }
            electionPolicy = new PreferredSingletonElectionPolicy(electionPolicy, preferences.toArray(new Preference[preferences.size()]));
        }

        final ServiceBuilder<?> builder = new SingletonDeploymentUnitPhaseServiceBuilder(name, container, electionPolicy).build(target);
        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }
        return Collections.<ServiceController<?>>singleton(builder.install());
    }
}

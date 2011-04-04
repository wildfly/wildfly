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

package org.jboss.as.server.deployment;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class DeploymentPhaseContextImpl extends SimpleAttachable implements DeploymentPhaseContext {
    private final ServiceTarget serviceTarget;
    private final ServiceRegistry serviceRegistry;
    private final ServiceBuilder<?> nextPhaseBuilder;
    private final DeploymentUnit deploymentUnitContext;
    private final Phase phase;

    DeploymentPhaseContextImpl(final ServiceTarget serviceTarget, final ServiceRegistry serviceRegistry, final ServiceBuilder<?> nextPhaseBuilder, final DeploymentUnit deploymentUnitContext, final Phase phase) {
        this.serviceTarget = serviceTarget;
        this.serviceRegistry = serviceRegistry;
        this.nextPhaseBuilder = nextPhaseBuilder;
        this.deploymentUnitContext = deploymentUnitContext;
        this.phase = phase;
    }

    public ServiceName getPhaseServiceName() {
        return deploymentUnitContext.getServiceName().append(phase.name());
    }

    public ServiceTarget getServiceTarget() {
        return serviceTarget;
    }

    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    public DeploymentUnit getDeploymentUnit() {
        return deploymentUnitContext;
    }

    public Phase getPhase() {
        return phase;
    }

    @Override
    public <T> void addDependency(ServiceName serviceName, AttachmentKey<T> attachmentKey) {
        addToAttachmentList(Attachments.NEXT_PHASE_ATTACHABLE_DEPS, new AttachableDependency(attachmentKey, serviceName, false));
    }

    @Override
    public <T> void addDependency(final ServiceName serviceName, final Class<T> type, final Injector<T> injector) {
        nextPhaseBuilder.addDependency(serviceName, type, injector);
    }

    @Override
    public <T> void addDeploymentDependency(ServiceName serviceName, AttachmentKey<T> attachmentKey) {
        addToAttachmentList(Attachments.NEXT_PHASE_ATTACHABLE_DEPS, new AttachableDependency(attachmentKey, serviceName, true));
    }
}

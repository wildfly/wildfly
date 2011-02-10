/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.server.deployment;

import org.jboss.msc.service.ServiceName;

/**
 * Represents a phase dependency that gets attached to the phase context or the deployment unit.
 *
 * @see DeploymentPhaseContext#addDependency(org.jboss.msc.service.ServiceName, AttachmentKey)
 * @see DeploymentPhaseContext#addDeploymentDependency(org.jboss.msc.service.ServiceName, AttachmentKey)
 * @author Stuart Douglas
 *
 */
public class AttachableDependency {

    private final AttachmentKey<?> attachmentKey;
    private final ServiceName serviceName;

    /**
     * True if this should be attached to the {@link DeploymentUnit}. Otherwise it is attached to the next
     * {@link DeploymentPhaseContext}.
     */
    private final boolean deploymentUnit;

    public AttachableDependency(AttachmentKey<?> attachmentKey, ServiceName serviceName, boolean deploymentUnit) {
        this.deploymentUnit = deploymentUnit;
        this.attachmentKey = attachmentKey;
        this.serviceName = serviceName;
    }

    public AttachmentKey<?> getAttachmentKey() {
        return attachmentKey;
    }

    public ServiceName getServiceName() {
        return serviceName;
    }

    public boolean isDeploymentUnit() {
        return deploymentUnit;
    }

}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.OperationContext;
import org.wildfly.clustering.service.Requirement;

/**
 * Abstract {@link CapabilityReferenceRecorder} containing logic common to attribute and resource capability references
 * @author Paul Ferraro
 */
public abstract class AbstractCapabilityReference implements CapabilityReferenceRecorder {

    private final Capability capability;
    private final Requirement requirement;

    protected AbstractCapabilityReference(Capability capability, Requirement requirement) {
        this.capability = capability;
        this.requirement = requirement;
    }

    @Override
    public String getBaseDependentName() {
        return this.capability.getName();
    }

    @Override
    public String getBaseRequirementName() {
        return this.requirement.getName();
    }

    protected String getDependentName(OperationContext context) {
        return this.capability.resolve(context.getCurrentAddress()).getName();
    }

    @Override
    public int hashCode() {
        return this.capability.getName().hashCode();
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof AbstractCapabilityReference) ? this.capability.getName().equals(((AbstractCapabilityReference) object).capability.getName()) : false;
    }
}

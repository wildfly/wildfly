/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

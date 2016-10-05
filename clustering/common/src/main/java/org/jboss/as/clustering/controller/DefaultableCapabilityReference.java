/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import java.util.Optional;

import org.wildfly.clustering.service.DefaultableBinaryRequirement;
import org.wildfly.clustering.service.DefaultableUnaryRequirement;

/**
 * {@link org.jboss.as.controller.CapabilityReferenceRecorder} that delegates to {@link Capability#resolve(org.jboss.as.controller.PathAddress)} to generate the name of the dependent capability
 * and uses a default requirement if the associated attribute is undefined.
 * @author Paul Ferraro
 */
public class DefaultableCapabilityReference extends CapabilityReference {

    /**
     * Creates a new reference between the specified capability and the specified requirement
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     */
    public DefaultableCapabilityReference(Capability capability, DefaultableUnaryRequirement requirement) {
        super(capability, requirement, (context, value) -> Optional.of((value != null) ? requirement.resolve(value) : requirement.getDefaultRequirement().getName()));
    }

    /**
     * Creates a new reference between the specified capability and the specified requirement
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     */
    public DefaultableCapabilityReference(Capability capability, DefaultableBinaryRequirement requirement) {
        super(capability, requirement, (context, value) -> Optional.of((value != null) ? requirement.resolve(context.getCurrentAddressValue(), value) : requirement.getDefaultRequirement().resolve(context.getCurrentAddressValue())));
    }
}

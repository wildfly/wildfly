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

package org.jboss.as.controller;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.ModelNodeRegistration;

/**
 * A basic extension context implementation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class NewExtensionContextImpl implements NewExtensionContext {
    private final ModelNodeRegistration profileRegistration;
    private final ModelNodeRegistration deploymentOverrideRegistration;

    /**
     * Construct a new instance.
     *
     * @param profileRegistration the profile registration
     * @param deploymentOverrideRegistration the deployment override registration
     */
    public NewExtensionContextImpl(final ModelNodeRegistration profileRegistration, final ModelNodeRegistration deploymentOverrideRegistration) {
        if (profileRegistration == null) {
            throw new IllegalArgumentException("profileRegistration is null");
        }
        if (deploymentOverrideRegistration == null) {
            throw new IllegalArgumentException("deploymentOverrideRegistration is null");
        }
        this.profileRegistration = profileRegistration;
        this.deploymentOverrideRegistration = deploymentOverrideRegistration;
    }

    /** {@inheritDoc} */
    @Override
    public SubsystemRegistration registerSubsystem(final String name) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        return new SubsystemRegistration() {
            @Override
            public ModelNodeRegistration registerSubsystemModel(final DescriptionProvider descriptionProvider) {
                if (descriptionProvider == null) {
                    throw new IllegalArgumentException("descriptionProvider is null");
                }
                return profileRegistration.registerSubModel(new PathElement("subsystem", name), descriptionProvider);
            }

            @Override
            public ModelNodeRegistration registerDeploymentModel(final DescriptionProvider descriptionProvider) {
                if (descriptionProvider == null) {
                    throw new IllegalArgumentException("descriptionProvider is null");
                }
                return deploymentOverrideRegistration.registerSubModel(new PathElement("configuration", name), descriptionProvider);
            }
        };
    }
}

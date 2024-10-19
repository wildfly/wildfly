/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.version.Stability;

/**
 * A subsystem resource registration.
 * @author Paul Ferraro
 */
public interface SubsystemResourceRegistration extends ResourceRegistration {

    /**
     * Returns the name of this subsystem.
     * @return the name of this subsystem.
     */
    String getName();

    @Override
    default PathElement getPathElement() {
        return PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, this.getName());
    }

    /**
     * Creates a new subsystem resource registration for the specified path.
     * @param path a path element
     * @return a resource registration
     */
    static SubsystemResourceRegistration of(String name) {
        return of(name, Stability.DEFAULT);
    }

    /**
     * Creates a new subsystem resource registration for the specified path and stability.
     * @param path a path element
     * @param stability a stability level
     * @return a resource registration
     */
    static SubsystemResourceRegistration of(String name, Stability stability) {
        return new DefaultSubsystemResourceRegistration(name, stability);
    }

    class DefaultSubsystemResourceRegistration implements SubsystemResourceRegistration {
        private final String name;
        private final Stability stability;

        DefaultSubsystemResourceRegistration(String name, Stability stability) {
            this.name = name;
            this.stability = stability;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public Stability getStability() {
            return this.stability;
        }
    }
}

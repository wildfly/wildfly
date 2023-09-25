/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;

/**
 * A configuration for an injection source.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class InjectionSource {
    /**
     * Get the value to use as the injection source.  The value will be yield an injectable which is dereferenced once
     * for every time the reference source is injected.  The given binder service builder may be used to apply any
     * dependencies for this binding (i.e. the source for the binding's value).
     *
     * @param resolutionContext the resolution context to use
     * @param serviceBuilder the builder for the binder service
     * @param phaseContext the deployment phase context
     * @param injector the injector into which the value should be placed
     * @throws DeploymentUnitProcessingException if an error occurs
     */
    public abstract void getResourceValue(ResolutionContext resolutionContext, ServiceBuilder<?> serviceBuilder, DeploymentPhaseContext phaseContext, Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException;

    /**
     * A resolution context for the injection source.
     */
    public static class ResolutionContext {
        private final boolean compUsesModule;
        private final String componentName;
        private final String moduleName;
        private final String applicationName;

        public ResolutionContext(final boolean compUsesModule, final String componentName, final String moduleName, final String applicationName) {
            this.compUsesModule = compUsesModule;
            this.componentName = componentName;
            this.moduleName = moduleName;
            this.applicationName = applicationName;
        }

        /**
         * Determine whether the resolution context has a combined "comp" and "module" namespace.
         *
         * @return {@code true} if "comp" is an alias for "module", {@code false} otherwise
         */
        public boolean isCompUsesModule() {
            return compUsesModule;
        }

        /**
         * Get the current component name, or {@code null} if there is none.
         *
         * @return the current component name
         */
        public String getComponentName() {
            return componentName;
        }

        /**
         * Get the current module name, or {@code null} if there is none.
         *
         * @return the current module name
         */
        public String getModuleName() {
            return moduleName;
        }

        /**
         * Get the current application name, or {@code null} if there is none.
         *
         * @return the current application name
         */
        public String getApplicationName() {
            return applicationName;
        }
    }
}

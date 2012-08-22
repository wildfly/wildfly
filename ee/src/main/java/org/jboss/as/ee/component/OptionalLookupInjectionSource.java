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

package org.jboss.as.ee.component;

import static org.jboss.as.ee.EeMessages.MESSAGES;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ContextListManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.value.ImmediateValue;

/**
 * An injection source that gets its value from an optional JNDI lookup.
 *
 * An optional dependency is added on the lookup value, and then the value
 * is obtained from an actual JNDI lookup. This means that the value injected
 * may change over the life of the application, e.g. if global env-entries
 * defined from another application are changed.
 *
 * If this item is not found, then nothing is injected
 *
 */
public final class OptionalLookupInjectionSource extends InjectionSource {
    private final String lookupName;

    public OptionalLookupInjectionSource(final String lookupName) {
        if (lookupName == null) {
            throw MESSAGES.nullVar("lookupName");
        }
        this.lookupName = lookupName;
    }

    /**
     * {@inheritDoc}
     */
    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) {
        final String applicationName = resolutionContext.getApplicationName();
        final String moduleName = resolutionContext.getModuleName();
        final String componentName = resolutionContext.getComponentName();
        final boolean compUsesModule = resolutionContext.isCompUsesModule();
        final String lookupName;
        if (!this.lookupName.contains("java:")) {
            if (componentName != null && !compUsesModule) {
                lookupName = "java:comp/env/" + this.lookupName;
            } else if (compUsesModule) {
                lookupName = "java:module/env/" + this.lookupName;
            } else {
                lookupName = "java:jboss/env" + this.lookupName;
            }
        } else if (this.lookupName.startsWith("java:comp/") && compUsesModule) {
            lookupName = "java:module/" + this.lookupName.substring(10);
        } else {
            lookupName = this.lookupName;
        }
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(applicationName, moduleName, componentName, lookupName);
        serviceBuilder.addDependency(ServiceBuilder.DependencyType.OPTIONAL, bindInfo.getBinderServiceName());
        injector.inject(new OptionalLookupManagedReferenceFactory(lookupName));
    }


    public boolean equals(Object configuration) {
        if (configuration instanceof OptionalLookupInjectionSource) {
            OptionalLookupInjectionSource lookup = (OptionalLookupInjectionSource) configuration;
            return lookupName.equals(lookup.lookupName);
        }
        return false;
    }

    public int hashCode() {
        return lookupName.hashCode();
    }

    private static class OptionalLookupManagedReferenceFactory implements ContextListAndJndiViewManagedReferenceFactory {
        private final String lookupName;

        public OptionalLookupManagedReferenceFactory(final String lookupName) {
            this.lookupName = lookupName;
        }

        @Override
        public ManagedReference getReference() {
            try {
                Object value = new InitialContext().lookup(lookupName);
                return new ValueManagedReference(new ImmediateValue<Object>(value));
            } catch (NamingException e) {
                return null;
            }
        }

        @Override
        public String getInstanceClassName() {
            final Object value = getReference().getInstance();
            return value != null ? value.getClass().getName() : ContextListManagedReferenceFactory.DEFAULT_INSTANCE_CLASS_NAME;
        }

        @Override
        public String getJndiViewInstanceValue() {
            return String.valueOf(getReference().getInstance());
        }
    }
}

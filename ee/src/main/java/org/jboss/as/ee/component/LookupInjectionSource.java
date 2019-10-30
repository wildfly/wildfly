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

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.naming.ImmediateManagedReference;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A binding which gets its value from another JNDI binding.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LookupInjectionSource extends InjectionSource {

    // the schemes of supported URLs
    private static final Set<String> URL_SCHEMES;

    static {
        Set<String> set = new HashSet<String>();
        set.add("http");
        set.add("https");
        set.add("ftp");
        set.add("file");
        set.add("jar");
        URL_SCHEMES = Collections.unmodifiableSet(set);
    }

    private final String lookupName;
    private final boolean optional;

    public LookupInjectionSource(final String lookupName) {
        this(lookupName,false);
    }

    public LookupInjectionSource(final String lookupName, final boolean optional) {
        if (lookupName == null) {
            throw EeLogger.ROOT_LOGGER.nullVar("lookupName");
        }
        this.lookupName = lookupName;
        this.optional = optional;
    }

    /**
     * {@inheritDoc}
     */
    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) {
        final String applicationName = resolutionContext.getApplicationName();
        final String moduleName = resolutionContext.getModuleName();
        final String componentName = resolutionContext.getComponentName();
        final boolean compUsesModule = resolutionContext.isCompUsesModule();
        final String scheme = org.jboss.as.naming.InitialContext.getURLScheme(lookupName);
        if (scheme == null) {
            // relative name, build absolute name and setup normal lookup injection
            if (componentName != null && !compUsesModule) {
                ContextNames.bindInfoFor(applicationName, moduleName, componentName, "java:comp/env/" + lookupName)
                        .setupLookupInjection(serviceBuilder, injector, phaseContext.getDeploymentUnit(), optional);
            } else if (compUsesModule) {
                ContextNames.bindInfoFor(applicationName, moduleName, componentName, "java:module/env/" + lookupName)
                        .setupLookupInjection(serviceBuilder, injector, phaseContext.getDeploymentUnit(), optional);
            } else {
                ContextNames.bindInfoFor(applicationName, moduleName, componentName, "java:jboss/env/" + lookupName)
                        .setupLookupInjection(serviceBuilder, injector, phaseContext.getDeploymentUnit(), optional);
            }
        } else {
            if (scheme.equals("java")) {
                // an absolute java name, setup normal lookup injection
                if (compUsesModule && lookupName.startsWith("java:comp/")) {
                    // switch "comp" with "module"
                    ContextNames.bindInfoFor(applicationName, moduleName, componentName, "java:module/" + lookupName.substring(10))
                            .setupLookupInjection(serviceBuilder, injector, phaseContext.getDeploymentUnit(), optional);
                } else {
                    ContextNames.bindInfoFor(applicationName, moduleName, componentName, lookupName)
                            .setupLookupInjection(serviceBuilder, injector, phaseContext.getDeploymentUnit(), optional);
                }
            } else {
                // an absolute non java name
                final ManagedReferenceFactory managedReferenceFactory;
                if (URL_SCHEMES.contains(scheme)) {
                    // a Java EE Standard Resource Manager Connection Factory for URLs, using lookup to define value of URL, inject factory that creates URL instances
                    managedReferenceFactory = new ManagedReferenceFactory() {
                        @Override
                        public ManagedReference getReference() {
                            try {
                                return new ImmediateManagedReference(new URL(lookupName));
                            } catch (MalformedURLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    };
                } else {
                    // lookup for a non java jndi resource, inject factory which does a true jndi lookup
                    managedReferenceFactory = new ManagedReferenceFactory() {
                        @Override
                        public ManagedReference getReference() {
                            try {
                                return new ImmediateManagedReference(new InitialContext().lookup(lookupName));
                            } catch (NamingException e) {
                                EeLogger.ROOT_LOGGER.tracef(e, "failed to lookup %s", lookupName);
                                return null;
                            }
                        }
                    };
                }
                injector.inject(managedReferenceFactory);
            }
        }
    }

    public boolean equals(Object configuration) {
        if (configuration instanceof LookupInjectionSource) {
            LookupInjectionSource lookup = (LookupInjectionSource) configuration;
            return lookupName.equals(lookup.lookupName);
        }
        return false;
    }

    public int hashCode() {
        return lookupName.hashCode();
    }

    public String toString() {
        return "lookup (" + lookupName + ")";
    }

}

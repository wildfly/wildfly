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

package org.jboss.as.ejb3.deployment.processors;

import static org.jboss.as.ee.component.Attachments.EE_APPLICATION_DESCRIPTION;

import java.util.HashSet;
import java.util.Set;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.EEApplicationDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.ViewManagedReferenceFactory;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.remote.RemoteViewManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Value;

/**
 * Implementation of {@link InjectionSource} responsible for finding a specific bean instance with a bean name and interface.
 *
 * @author John Bailey
 * @author Stuart Douglas
 */
public class EjbInjectionSource extends InjectionSource {
    private final String beanName;
    private final String typeName;
    private final String bindingName;
    private final DeploymentUnit deploymentUnit;
    private final boolean appclient;
    private volatile String error = null;
    private volatile ServiceName resolvedViewName;
    private volatile RemoteViewManagedReferenceFactory remoteFactory;
    private volatile boolean resolved = false;

    public EjbInjectionSource(final String beanName, final String typeName, final String bindingName, final DeploymentUnit deploymentUnit, final boolean appclient) {
        this.beanName = beanName;
        this.typeName = typeName;
        this.bindingName = bindingName;
        this.deploymentUnit = deploymentUnit;
        this.appclient = appclient;
    }

    public EjbInjectionSource(final String typeName, final String bindingName, final DeploymentUnit deploymentUnit, final boolean appclient) {
        this.bindingName = bindingName;
        this.deploymentUnit = deploymentUnit;
        this.appclient = appclient;
        this.beanName = null;
        this.typeName = typeName;
    }

    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        resolve();

        if (error != null) {
            throw new DeploymentUnitProcessingException(error);
        }

        if (remoteFactory != null) {
            //because we are using the ejb: lookup namespace we do not need a dependency
            injector.inject(remoteFactory);
        } else if (!appclient) {
            //we do not add a dependency if this is the appclient
            //as local injections are simply ignored
            serviceBuilder.addDependency(resolvedViewName, ComponentView.class, new ViewManagedReferenceFactory.Injector(injector));
        }
    }

    /**
     * Checks if this ejb injection has been resolved yet, and if not resolves it.
     */
    private void resolve() {
        if (!resolved) {
            synchronized (this) {
                if (!resolved) {

                    final Set<ViewDescription> views = getViews();

                    final Set<EJBViewDescription> ejbsForViewName = new HashSet<EJBViewDescription>();
                    for (final ViewDescription view : views) {
                        if (view instanceof EJBViewDescription) {
                            final MethodIntf viewType = ((EJBViewDescription) view).getMethodIntf();
                            // @EJB injection *shouldn't* consider the @WebService endpoint view or MDBs
                            if (viewType == MethodIntf.SERVICE_ENDPOINT || viewType == MethodIntf.MESSAGE_ENDPOINT) {
                                continue;
                            }
                            ejbsForViewName.add((EJBViewDescription) view);
                        }
                    }


                    if (ejbsForViewName.isEmpty()) {
                        if (beanName == null) {
                            error = EjbLogger.ROOT_LOGGER.ejbNotFound(typeName, bindingName);
                        } else {
                            error = EjbLogger.ROOT_LOGGER.ejbNotFound(typeName, beanName, bindingName);
                        }
                    } else if (ejbsForViewName.size() > 1) {
                        if (beanName == null) {
                            error = EjbLogger.ROOT_LOGGER.moreThanOneEjbFound(typeName, bindingName, ejbsForViewName);
                        } else {
                            error = EjbLogger.ROOT_LOGGER.moreThanOneEjbFound(typeName, beanName, bindingName, ejbsForViewName);
                        }
                    } else {
                        final EJBViewDescription description = ejbsForViewName.iterator().next();
                        final EJBViewDescription ejbViewDescription = (EJBViewDescription) description;
                        //for remote interfaces we do not want to use a normal binding
                        //we need to bind the remote proxy factory into JNDI instead to get the correct behaviour

                        if (ejbViewDescription.getMethodIntf() == MethodIntf.REMOTE || ejbViewDescription.getMethodIntf() == MethodIntf.HOME) {
                            final EJBComponentDescription componentDescription = (EJBComponentDescription) description.getComponentDescription();
                            final EEModuleDescription moduleDescription = componentDescription.getModuleDescription();
                            final String earApplicationName = moduleDescription.getEarApplicationName();
                            final Value<ClassLoader> viewClassLoader = new Value<ClassLoader>() {
                                @Override
                                public ClassLoader getValue() throws IllegalStateException, IllegalArgumentException {
                                    final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
                                    return module != null ? module.getClassLoader() : null;
                                }
                            };
                            remoteFactory = new RemoteViewManagedReferenceFactory(earApplicationName, moduleDescription.getModuleName(), moduleDescription.getDistinctName(), componentDescription.getComponentName(), description.getViewClassName(), componentDescription.isStateful(), viewClassLoader, appclient);
                        }
                        final ServiceName serviceName = description.getServiceName();
                        resolvedViewName = serviceName;
                    }
                    resolved = true;
                }
            }
        }
    }

    private Set<ViewDescription> getViews() {
        final EEApplicationDescription applicationDescription = deploymentUnit.getAttachment(EE_APPLICATION_DESCRIPTION);
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final Set<ViewDescription> componentsForViewName;
        if (beanName != null) {
            componentsForViewName = applicationDescription.getComponents(beanName, typeName, deploymentRoot.getRoot());
        } else {
            componentsForViewName = applicationDescription.getComponentsForViewName(typeName, deploymentRoot.getRoot());
        }
        return componentsForViewName;
    }

    public boolean equals(Object o) {
        if (this == o) { return true; }

        if (!(o instanceof EjbInjectionSource)) { return false; }

        resolve();

        if (error != null) {
            //we can't do a real equals comparison in this case, so just return false
            return false;
        }

        final EjbInjectionSource other = (EjbInjectionSource) o;
        return eq(typeName, other.typeName) && eq(resolvedViewName, other.resolvedViewName);
    }

    public int hashCode() {
        return typeName.hashCode();
    }

    private static boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}

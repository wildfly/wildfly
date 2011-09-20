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
package org.jboss.as.webservices.webserviceref;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import javax.naming.Referenceable;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEApplicationDescription;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.value.Value;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.classloading.ClassLoaderProvider;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedServiceRefMetaData;
import org.jboss.wsf.spi.serviceref.ServiceRefHandler;
import org.jboss.wsf.spi.serviceref.ServiceRefHandlerFactory;
import org.jboss.wsf.stack.cxf.client.serviceref.CXFServiceObjectFactoryJAXWS;

/**
 * WebServiceRef injection source.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class WSRefValueSource extends InjectionSource implements Value<Object> {
    private final Module module;
    private final UnifiedServiceRefMetaData serviceRef;

    WSRefValueSource(Module module, UnifiedServiceRefMetaData serviceRef) {
        this.module = module;
        this.serviceRef = serviceRef;
    }

    public void getResourceValue(final ResolutionContext resolutionContext, ServiceBuilder<?> serviceBuilder, DeploymentPhaseContext phaseContext, Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEApplicationDescription applicationComponentDescription = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_DESCRIPTION);
        if (applicationComponentDescription == null) {
            return; // Not an EE deployment
        }
        ManagedReferenceFactory factory = new ValueManagedReferenceFactory(this);
        serviceBuilder.addInjection(injector, factory);
    }

    public Object getValue() throws IllegalStateException, IllegalArgumentException {
        // FIXME this is a workaround to class loader issues
        final ClassLoader tccl = ClassLoaderProvider.getDefaultProvider().getServerIntegrationClassLoader();
        final ClassLoader classLoader = new ClassLoader(this.getClass().getClassLoader()) {
            @Override
            public Class<?> loadClass(String className) throws ClassNotFoundException {
                try {
                    return super.loadClass(className);
                } catch (ClassNotFoundException cnfe) {
                    return module.getClassLoader().loadClass(className);
                }
            }

            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                final Enumeration<URL> superResources = super.getResources(name);
                final Enumeration<URL> duModuleCLResources = module.getClassLoader().getResources(name);
                if (superResources == null || !superResources.hasMoreElements()) {
                    return duModuleCLResources;
                }
                if (duModuleCLResources == null || !duModuleCLResources.hasMoreElements()) {
                    return superResources;
                }
                return new Enumeration<URL>() {
                    public boolean hasMoreElements() {
                        return superResources.hasMoreElements() || duModuleCLResources.hasMoreElements();
                    }

                    public URL nextElement() {
                        if (superResources.hasMoreElements()) {
                            return superResources.nextElement();
                        }
                        return duModuleCLResources.nextElement();
                    }
                };
            }
        };
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            return new CXFServiceObjectFactoryJAXWS().getObjectInstance(getReferenceable(), null, null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    private Referenceable getReferenceable() {
        // FIXME SPIProviderResolver won't require a TCCL in the future
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        final Referenceable referenceable;
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            final SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
            final ServiceRefHandler serviceRefHandler = spiProvider.getSPI(ServiceRefHandlerFactory.class).getServiceRefHandler();
            return serviceRefHandler.createReferenceable(serviceRef);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }
}
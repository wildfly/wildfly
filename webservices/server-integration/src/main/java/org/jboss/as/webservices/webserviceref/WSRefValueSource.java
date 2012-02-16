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

import static org.jboss.as.webservices.webserviceref.SecurityActions.getContextClassLoader;
import static org.jboss.as.webservices.webserviceref.SecurityActions.setContextClassLoader;

import javax.naming.Referenceable;
import javax.naming.spi.ObjectFactory;

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.value.Value;
import org.jboss.ws.common.utils.DelegateClassLoader;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.classloading.ClassLoaderProvider;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedServiceRefMetaData;
import org.jboss.wsf.spi.serviceref.ServiceRefHandler;
import org.jboss.wsf.spi.serviceref.ServiceRefHandler.Type;
import org.jboss.wsf.spi.serviceref.ServiceRefHandlerFactory;

/**
 * WebServiceRef injection source.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class WSRefValueSource extends InjectionSource implements Value<Object> {
    private final UnifiedServiceRefMetaData serviceRef;
    private final ClassLoader classLoader;

    WSRefValueSource(final UnifiedServiceRefMetaData serviceRef, final ClassLoader classLoader) {
        this.serviceRef = serviceRef;
        this.classLoader = classLoader;
    }

    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final ManagedReferenceFactory factory = new ValueManagedReferenceFactory(this);
        serviceBuilder.addInjection(injector, factory);
    }

    public Object getValue() throws IllegalStateException, IllegalArgumentException {
        final ClassLoader oldCL = getContextClassLoader();
        try {
            final ClassLoader integrationCL = new DelegateClassLoader(getClassLoader(), classLoader);
            setContextClassLoader(integrationCL);
            final Referenceable referenceable = getReferenceable(integrationCL);
            final Class<?> clazz = Class.forName(referenceable.getReference().getFactoryClassName(), true, integrationCL);
            final ObjectFactory factory = (ObjectFactory)clazz.newInstance();
            return factory.getObjectInstance(referenceable.getReference(), null, null, null);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            setContextClassLoader(oldCL);
        }
    }

    private ClassLoader getClassLoader() {
        ClassLoaderProvider provider = ClassLoaderProvider.getDefaultProvider();
        if (!Type.JAXRPC.equals(serviceRef.getType())) {
            return provider.getServerIntegrationClassLoader();
        } else {
            return provider.getServerJAXRPCIntegrationClassLoader();
        }
    }

    private Referenceable getReferenceable(final ClassLoader loader) {
        final SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
        final ServiceRefHandler serviceRefHandler = spiProvider.getSPI(ServiceRefHandlerFactory.class, loader).getServiceRefHandler();
        return serviceRefHandler.createReferenceable(serviceRef);
    }

}
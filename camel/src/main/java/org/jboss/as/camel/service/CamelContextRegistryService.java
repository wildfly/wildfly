/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.camel.service;

import static org.jboss.as.camel.CamelMessages.MESSAGES;

import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.jboss.as.camel.CamelConstants;
import org.jboss.as.camel.CamelContextRegistry;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.Services;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * The {@link CamelContextRegistry} service
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public class CamelContextRegistryService extends AbstractService<CamelContextRegistry> {

    private final InjectedValue<BundleContext> injectedSystemContext = new InjectedValue<BundleContext>();
    private CamelContextRegistry camelContextRegistry;

    public static ServiceController<CamelContextRegistry> addService(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        CamelContextRegistryService service = new CamelContextRegistryService();
        ServiceBuilder<CamelContextRegistry> builder = serviceTarget.addService(CamelConstants.CAMEL_CONTEXT_REGISTRY_NAME, service);
        builder.addDependency(Services.FRAMEWORK_ACTIVE, BundleContext.class, service.injectedSystemContext);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    // Hide ctor
    private CamelContextRegistryService() {
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        BundleContext syscontext = injectedSystemContext.getValue();
        camelContextRegistry = new OSGiCamelContextRegistry(syscontext);
    }

    @Override
    public CamelContextRegistry getValue() {
        return camelContextRegistry;
    }

    static class OSGiCamelContextRegistry implements CamelContextRegistry {

        private final BundleContext syscontext;
        private Map<String, ServiceRegistration<CamelContext>> registrations = new HashMap<String, ServiceRegistration<CamelContext>>();

        OSGiCamelContextRegistry(BundleContext syscontext) {
            this.syscontext = syscontext;
        }

        @Override
        public CamelContext getCamelContext(String name) {
            try {
                ServiceReference<CamelContext> sref = getCamelContextReference(name);
                return sref != null ? syscontext.getService(sref) : null;
            } catch (InvalidSyntaxException ex) {
                throw MESSAGES.illegalCamelContextName(name);
            }
        }

        @Override
        public synchronized void registerCamelContext(CamelContext camelctx) {
            String contextName = camelctx.getName();
            if (registrations.get(contextName) != null)
                throw MESSAGES.camelContextAlreadyRegistered(contextName);
            Hashtable<String, String> properties = new Hashtable<String, String>();
            properties.put(CamelConstants.CAMEL_CONTEXT_NAME_KEY, contextName);
            ServiceRegistration<CamelContext> sreg = syscontext.registerService(CamelContext.class, camelctx, properties);
            registrations.put(contextName, sreg);
        }

        @Override
        public synchronized CamelContext unregisterCamelContext(String name) {
            CamelContext camelctx = null;
            ServiceRegistration<CamelContext> sreg = registrations.remove(name);
            if (sreg != null) {
                camelctx = syscontext.getService(sreg.getReference());
                sreg.unregister();
            }
            return camelctx;
        }

        private ServiceReference<CamelContext> getCamelContextReference(String name) throws InvalidSyntaxException {
            String filter = "(" + CamelConstants.CAMEL_CONTEXT_NAME_KEY + "=" + name + ")";
            Collection<ServiceReference<CamelContext>> srefs = syscontext.getServiceReferences(CamelContext.class, filter);
            return srefs.size() > 0 ? srefs.iterator().next() : null;
        }
    }
}

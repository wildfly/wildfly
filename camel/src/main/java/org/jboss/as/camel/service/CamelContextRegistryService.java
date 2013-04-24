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

import static org.jboss.as.camel.CamelLogger.LOGGER;
import static org.jboss.as.camel.CamelMessages.MESSAGES;

import java.util.Collection;
import java.util.Hashtable;

import org.apache.camel.CamelContext;
import org.jboss.as.camel.CamelConstants;
import org.jboss.as.camel.CamelContextFactory;
import org.jboss.as.camel.CamelContextRegistry;
import org.jboss.as.camel.parser.SubsystemState;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.Services;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * The {@link CamelContextRegistry} service
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public class CamelContextRegistryService extends AbstractService<CamelContextRegistry> {

    private static final String SPRING_BEANS_HEADER = "<beans " +
            "xmlns='http://www.springframework.org/schema/beans' " +
            "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
            "xsi:schemaLocation='http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd " +
            "http://camel.apache.org/schema/spring http://camel.apache.org/schema/spring/camel-spring.xsd'>";

    private final InjectedValue<BundleContext> injectedSystemContext = new InjectedValue<BundleContext>();
    private final InjectedValue<SubsystemState> injectedSubsystemState = new InjectedValue<SubsystemState>();
    private CamelContextRegistry camelContextRegistry;

    public static ServiceController<CamelContextRegistry> addService(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        CamelContextRegistryService service = new CamelContextRegistryService();
        ServiceBuilder<CamelContextRegistry> builder = serviceTarget.addService(CamelConstants.CAMEL_CONTEXT_REGISTRY_NAME, service);
        builder.addDependency(SubsystemStateService.SERVICE_NAME, SubsystemState.class, service.injectedSubsystemState);
        builder.addDependency(Services.FRAMEWORK_ACTIVE, BundleContext.class, service.injectedSystemContext);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    // Hide ctor
    private CamelContextRegistryService() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void start(StartContext startContext) throws StartException {
        final BundleContext syscontext = injectedSystemContext.getValue();
        final ServiceTarget serviceTarget = startContext.getChildTarget();
        camelContextRegistry = new DefaultCamelContextRegistry(serviceTarget, syscontext);

        final SubsystemState subsystemState = injectedSubsystemState.getValue();
        for (final String name : subsystemState.getContextDefinitionNames()) {
            LOGGER.infoRegisterCamelContext(name);

            // Register the {@link CamelContext} as OSGi {@link ServiceFactory}
            ServiceFactory<CamelContext> factory = new ServiceFactory<CamelContext>() {
                private CamelContext camelContext;
                @Override
                public CamelContext getService(Bundle bundle, ServiceRegistration<CamelContext> registration) {
                    if (camelContext == null) {
                        try {
                            ClassLoader classLoader = CamelContextRegistry.class.getClassLoader();
                            String beansXML = getBeansXML(name, subsystemState.getContextDefinition(name));
                            camelContext = CamelContextFactory.createSpringCamelContext(beansXML.getBytes(), classLoader);
                        } catch (Exception ex) {
                            throw MESSAGES.cannotCreateCamelContext(ex, name);
                        }
                    }
                    return camelContext;
                }

                @Override
                public void ungetService(Bundle bundle, ServiceRegistration<CamelContext> registration, CamelContext service) {
                }
            };
            Hashtable<String, String> properties = new Hashtable<String, String>();
            properties.put(CamelConstants.CAMEL_CONTEXT_NAME_KEY, name);
            final ServiceRegistration<?> sreg = syscontext.registerService(CamelContext.class.getName(), factory, properties);
            final ServiceReference<CamelContext> sref = (ServiceReference<CamelContext>) sreg.getReference();

            // Install the {@link CamelContext} as ON_DEMAND {@link Service}
            Service<CamelContext> service = new AbstractService<CamelContext>(){
                private CamelContext camelContext;

                @Override
                public void start(StartContext context) throws StartException {
                    BundleContext syscontext = injectedSystemContext.getValue();
                    camelContext = syscontext.getService(sref);
                }

                @Override
                public CamelContext getValue() {
                    return camelContext;
                }
            };
            ServiceName serviceName = CamelConstants.CAMEL_CONTEXT_BASE_NAME.append(name);
            ServiceBuilder<CamelContext> builder = serviceTarget.addService(serviceName, service);
            builder.setInitialMode(Mode.ON_DEMAND);
            builder.install();
        }
    }

    @Override
    public CamelContextRegistry getValue() {
        return camelContextRegistry;
    }

    private String getBeansXML(String name, String contextDefinition) {
        // [TODO] allow expressions in system context definition
        String hashReplaced = contextDefinition.replace("#{", "${");
        return SPRING_BEANS_HEADER + "<camelContext id='" + name + "' xmlns='http://camel.apache.org/schema/spring'>" + hashReplaced + "</camelContext></beans>";
    }

    static class DefaultCamelContextRegistry implements CamelContextRegistry {

        private final ServiceTarget serviceTarget;
        private final BundleContext syscontext;

        DefaultCamelContextRegistry(ServiceTarget serviceTarget, BundleContext syscontext) {
            this.serviceTarget = serviceTarget;
            this.syscontext = syscontext;
        }

        @Override
        public CamelContext getCamelContext(String name) {
            ServiceReference<CamelContext> sref = getCamelContextReference(name);
            return sref != null ? syscontext.getService(sref) : null;
        }

        @Override
        public CamelContextRegistration registerCamelContext(CamelContext camelctx) {
            String name = camelctx.getName();
            if (getCamelContextReference(name) != null)
                throw MESSAGES.camelContextAlreadyRegistered(name);

            LOGGER.infoRegisterCamelContext(name);

            // Register the {@link CamelContext} as OSGi {@link ServiceFactory}
            Hashtable<String, String> properties = new Hashtable<String, String>();
            properties.put(CamelConstants.CAMEL_CONTEXT_NAME_KEY, name);
            final ServiceRegistration<CamelContext> sreg = syscontext.registerService(CamelContext.class, camelctx, properties);

            // Install the {@link CamelContext} as {@link Service}
            ServiceName serviceName = CamelConstants.CAMEL_CONTEXT_BASE_NAME.append(name);
            ValueService<CamelContext> service = new ValueService<CamelContext>(new ImmediateValue<CamelContext>(camelctx));
            final ServiceController<CamelContext> controller = serviceTarget.addService(serviceName, service).install();

            return new CamelContextRegistration() {
                @Override
                public void unregister() {
                    controller.setMode(Mode.REMOVE);
                    sreg.unregister();
                }
            };
        }

        private ServiceReference<CamelContext> getCamelContextReference(String name) {
            try {
                String filter = "(" + CamelConstants.CAMEL_CONTEXT_NAME_KEY + "=" + name + ")";
                Collection<ServiceReference<CamelContext>> srefs = syscontext.getServiceReferences(CamelContext.class, filter);
                return srefs.size() > 0 ? srefs.iterator().next() : null;
            } catch (InvalidSyntaxException ex) {
                throw MESSAGES.illegalCamelContextName(name);
            }
        }
    }
}

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.osgi.service;

import static org.jboss.as.osgi.OSGiConstants.SERVICE_BASE_NAME;
import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.osgi.OSGiMessages.MESSAGES;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.modules.Module;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.AbstractBundleRevisionAdaptor;
import org.jboss.osgi.framework.IntegrationService;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.resolver.XBundleRevision;
import org.jboss.osgi.resolver.XBundleRevisionBuilderFactory;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.osgi.framework.BundleContext;

/**
 * A service that tracks module registrations.
 *
 * @author thomas.diesler@jboss.com
 * @since 25-Jul-2012
 */
public class ModuleRegistrationTracker extends AbstractService<Void> {

    public static final ServiceName MODULE_REGISTRATION_COMPLETE = SERVICE_BASE_NAME.append("module", "registration");

    private final InjectedValue<BundleContext> injectedSystemContext = new InjectedValue<BundleContext>();
    private final InjectedValue<XEnvironment> injectedEnvironment = new InjectedValue<XEnvironment>();

    private final Map<Module, Registration> registrations = new LinkedHashMap<Module, Registration>();

    public ServiceController<Void> install(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        ServiceBuilder<Void> builder = serviceTarget.addService(MODULE_REGISTRATION_COMPLETE, this);
        builder.addDependency(Services.FRAMEWORK_CREATE, BundleContext.class, injectedSystemContext);
        builder.addDependency(Services.ENVIRONMENT, XEnvironment.class, injectedEnvironment);
        builder.addDependencies(IntegrationService.BOOTSTRAP_BUNDLES_COMPLETE);
        builder.addListener(verificationHandler);
        builder.setInitialMode(Mode.ON_DEMAND);
        return builder.install();
    }

    public synchronized void registerModule(Module module, OSGiMetaData metadata) {
        BundleContext context = injectedSystemContext.getOptionalValue();
        XEnvironment env = injectedEnvironment.getOptionalValue();
        Registration reg = new Registration(module, metadata);
        if (context != null && env != null) {
            registerInternal(context, env, reg);
        }
        registrations.put(module, reg);
    }

    public synchronized void unregisterModule(Module module) {
        BundleContext context = injectedSystemContext.getOptionalValue();
        XEnvironment env = injectedEnvironment.getOptionalValue();
        Registration reg = registrations.remove(module);
        if (context != null && env != null && reg != null) {
            unregisterInternal(env, reg);
        }
    }

    public synchronized void start(StartContext startContext) throws StartException {
        ServiceController<?> serviceController = startContext.getController();
        LOGGER.tracef("Starting: %s in mode %s", serviceController.getName(), serviceController.getMode());

        BundleContext syscontext = injectedSystemContext.getValue();
        XEnvironment env = injectedEnvironment.getValue();
        for (Registration reg : registrations.values()) {
            registerInternal(syscontext, env, reg);
        }
        registrations.clear();
    }

    private XBundleRevision registerInternal(final BundleContext context, final XEnvironment env, final Registration reg) {
        final OSGiMetaData metadata = reg.metadata;
        final Module module = reg.module;

        LOGGER.infoRegisterModule(module.getIdentifier());
        XBundleRevisionBuilderFactory factory = new XBundleRevisionBuilderFactory() {
            @Override
            public XBundleRevision createResource() {
                return new AbstractBundleRevisionAdaptor(context, module);
            }
        };

        try {
            XResourceBuilder builder = XBundleRevisionBuilderFactory.create(factory);
            if (metadata != null) {
                builder.loadFrom(metadata);
            } else {
                builder.loadFrom(module);
            }
            reg.brev = (XBundleRevision) builder.getResource();
            env.installResources(reg.brev);
        } catch (Throwable th) {
            throw MESSAGES.illegalStateFailedToRegisterModule(th, module);
        }

        return reg.brev;
    }

    private void unregisterInternal(final XEnvironment env, final Registration reg) {
        assert reg.brev != null : "BundleRevision not null";
        LOGGER.infoUnregisterModule(reg.module.getIdentifier());
        env.uninstallResources(reg.brev);
    }

    public static final class Registration {
        private final OSGiMetaData metadata;
        private final Module module;
        private XBundleRevision brev;

        Registration(Module module, OSGiMetaData metadata) {
            this.metadata = metadata;
            this.module = module;
        }
    }
}
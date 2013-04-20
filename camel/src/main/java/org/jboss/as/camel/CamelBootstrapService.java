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

package org.jboss.as.camel;

import static org.jboss.as.camel.CamelLogger.LOGGER;
import static org.jboss.as.camel.CamelMessages.MESSAGES;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.framework.spi.AbstractBundleRevisionAdaptor;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XBundleRevision;
import org.jboss.osgi.resolver.XBundleRevisionBuilder;
import org.jboss.osgi.resolver.XBundleRevisionBuilderFactory;
import org.jboss.osgi.resolver.XEnvironment;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * Service responsible for creating and managing the life-cycle of the Camel subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public class CamelBootstrapService extends AbstractService<Void> {

    public static final ModuleIdentifier CAMEL_MODULE_IDENTIFIER = ModuleIdentifier.create("org.apache.camel");
    public static final ServiceName CAMEL_NAME = ServiceName.JBOSS.append("Camel");

    private final InjectedValue<BundleContext> injectedSystemContext = new InjectedValue<BundleContext>();
    private final InjectedValue<XEnvironment> injectedEnvironment = new InjectedValue<XEnvironment>();

    public static ServiceController<Void> addService(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        CamelBootstrapService service = new CamelBootstrapService();
        ServiceBuilder<Void> builder = serviceTarget.addService(CAMEL_NAME, service);
        builder.addDependency(Services.ENVIRONMENT, XEnvironment.class, service.injectedEnvironment);
        builder.addDependency(Services.FRAMEWORK_ACTIVE, BundleContext.class, service.injectedSystemContext);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    // Hide ctor
    private CamelBootstrapService() {
    }

    @Override
    public void start(StartContext context) throws StartException {
        LOGGER.infoActivatingSubsystem();

        final BundleContext syscontext = injectedSystemContext.getValue();
        final XEnvironment env = injectedEnvironment.getValue();

        // Load the org.apache.camel module
        final Module camelModule;
        try {
            ModuleLoader moduleLoader = Module.getCallerModuleLoader();
            camelModule = moduleLoader.loadModule(CAMEL_MODULE_IDENTIFIER);
        } catch (ModuleLoadException ex) {
            throw MESSAGES.cannotObtainCamelModule(ex, CAMEL_MODULE_IDENTIFIER);
        }

        // Build the {@link OSGiMetaData} and register the camel bundle with the {@link XEnvironment}
        XBundle camelBundle;
        try {
            OSGiMetaData metadata = getOSGiMetadata(camelModule);
            XBundleRevisionBuilder builder = XBundleRevisionBuilderFactory.create(new XBundleRevisionBuilderFactory() {
                @Override
                public XBundleRevision createResource() {
                    return new AbstractBundleRevisionAdaptor(syscontext, camelModule);
                }
            });
            XBundleRevision brev = builder.loadFrom(metadata).getResource();
            env.installResources(brev);
            camelBundle = brev.getBundle();
        } catch (IOException ex) {
            throw MESSAGES.cannotRegisterCamelBundle(ex);
        }

        // Start the camel bundle
        try {
            camelBundle.start();
        } catch (BundleException ex) {
            throw MESSAGES.cannotStartCamelBundle(ex, camelBundle);
        }
    }

    private OSGiMetaData getOSGiMetadata(Module module) throws IOException {
        URL manifestURL = module.getExportedResource(JarFile.MANIFEST_NAME);
        InputStream input = manifestURL.openStream();
        try {
            Manifest manifest = new Manifest(input);
            return OSGiMetaDataBuilder.load(manifest);
        } finally {
            input.close();
        }
    }
}

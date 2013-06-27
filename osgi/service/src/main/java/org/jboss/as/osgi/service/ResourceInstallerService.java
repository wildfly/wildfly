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

package org.jboss.as.osgi.service;


import java.io.IOException;

import org.jboss.as.osgi.OSGiConstants;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.framework.spi.AbstractBundleRevisionAdaptor;
import org.jboss.osgi.framework.spi.IntegrationConstants;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.repository.RepositoryMessages;
import org.jboss.osgi.repository.ResourceInstaller;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.spi.AbstractResourceInstaller;
import org.jboss.osgi.repository.spi.ModuleIdentityRepository;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XBundleRevision;
import org.jboss.osgi.resolver.XBundleRevisionBuilderFactory;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/**
 * The {@link ResourceInstaller} integration
 *
 * @author Thomas.Diesler@jboss.com
 * @since 10-May-2013
 */
public class ResourceInstallerService extends AbstractResourceInstaller implements Service<ResourceInstaller> {

    public static final ServiceName SERVICE_NAME = OSGiConstants.SERVICE_BASE_NAME.append("ResourceInstaller");

    private final InjectedValue<XRepository> injectedRepository = new InjectedValue<XRepository>();
    private final InjectedValue<XEnvironment> injectedEnvironment = new InjectedValue<XEnvironment>();

    public static ServiceController<ResourceInstaller> addService(final ServiceTarget target) {
        ResourceInstallerService service = new ResourceInstallerService();
        ServiceBuilder<ResourceInstaller> builder = target.addService(SERVICE_NAME, service);
        builder.addDependency(OSGiConstants.REPOSITORY_SERVICE_NAME, XRepository.class, service.injectedRepository);
        builder.addDependency(Services.ENVIRONMENT, XEnvironment.class, service.injectedEnvironment);
        return builder.install();
    }

    private ResourceInstallerService() {
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public ResourceInstaller getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public XBundle installModuleResource(final BundleContext context, final XResource res) throws BundleException {
        XIdentityCapability icap = res.getIdentityCapability();
        if (!icap.getNamespace().equals(IdentityNamespace.IDENTITY_NAMESPACE))
            throw RepositoryMessages.MESSAGES.unsupportedResource(res);

        String idspec = (String) icap.getAttribute(XResource.MODULE_IDENTITY_NAMESPACE);
        ModuleIdentifier moduleId = ModuleIdentifier.fromString(idspec);

        // Build the bundle revision
        final XRepository repository = injectedRepository.getValue();
        final ModuleIdentityRepository moduleRepository = repository.adapt(ModuleIdentityRepository.class);
        final Module module = moduleRepository.loadModule(moduleId);
        XBundleRevisionBuilderFactory factory = new XBundleRevisionBuilderFactory() {
            @Override
            public XBundleRevision createResource() {
                return new AbstractBundleRevisionAdaptor(context, module);
            }
        };
        XResourceBuilder<XBundleRevision> builder = XBundleRevisionBuilderFactory.create(factory);
        for (Capability cap : res.getCapabilities(null)) {
            builder.addCapability(cap.getNamespace(), cap.getAttributes(), cap.getDirectives());
        }
        for (Requirement req : res.getRequirements(null)) {
            builder.addRequirement(req.getNamespace(), req.getAttributes(), req.getDirectives());
        }
        XBundleRevision brev = builder.getResource();

        // Get and attach the OSGi metadata
        try {
            OSGiMetaData metadata = moduleRepository.getOSGiMetaData(brev);
            brev.putAttachment(IntegrationConstants.OSGI_METADATA_KEY, metadata);
        } catch (IOException e) {
            throw RepositoryMessages.MESSAGES.cannotObtainResourceMetadata(brev);
        }

        // Install the resource into the environment
        XEnvironment environment = injectedEnvironment.getValue();
        environment.installResources(brev);

        return brev.getBundle();
    }
}
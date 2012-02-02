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

import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.repository.ArtifactProviderPlugin;
import org.jboss.osgi.repository.RepositoryCachePlugin;
import org.jboss.osgi.repository.core.FileBasedRepositoryCachePlugin;
import org.jboss.osgi.repository.core.MavenArtifactProvider;
import org.jboss.osgi.repository.core.RepositoryImpl;
import org.jboss.osgi.repository.core.TrackingArtifactProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.repository.Repository;

import java.io.File;

import static org.jboss.as.osgi.service.FrameworkBootstrapService.SERVICE_BASE_NAME;

/**
 * An service that provides the {@link Repository} service to the OSGi system context
 *
 * @author thomas.diesler@jboss.com
 * @since 02-Feb-2012
 */
final class RepositoryProvider extends AbstractService<Repository> {

    public static final ServiceName SERVICE_NAME = SERVICE_BASE_NAME.append("repository.provider");

    private final InjectedValue<BundleContext> injectedSystemContext = new InjectedValue<BundleContext>();
    private Repository repository;

    static ServiceController<?> addService(final ServiceTarget target) {
        RepositoryProvider service = new RepositoryProvider();
        ServiceBuilder<?> builder = target.addService(SERVICE_NAME, service);
        builder.addDependency(Services.SYSTEM_CONTEXT, BundleContext.class, service.injectedSystemContext);
        builder.addDependency(Services.FRAMEWORK_CREATE);
        builder.setInitialMode(Mode.PASSIVE);
        return builder.install();
    }

    private RepositoryProvider() {
    }

    @Override
    public void start(StartContext context) throws StartException {
        BundleContext syscontext = injectedSystemContext.getValue();
        // Register the MavenArtifactProvider
        ArtifactProviderPlugin provider = new MavenArtifactProvider();
        syscontext.registerService(ArtifactProviderPlugin.class.getName(), provider, null);
        // Create the RepositoryCachePlugin
        File cacheFile = syscontext.getDataFile("repository");
        RepositoryCachePlugin cache = new FileBasedRepositoryCachePlugin(cacheFile);
        // Register the Repository
        repository = new RepositoryImpl(new TrackingArtifactProvider(syscontext), cache);
        syscontext.registerService(Repository.class.getName(), repository, null);
    }

    @Override
    public Repository getValue() throws IllegalStateException {
        return repository;
    }

    @Override
    public String toString() {
        return RepositoryProvider.class.getSimpleName();
    }
}

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

import static org.jboss.osgi.repository.XRepository.MODULE_IDENTITY_NAMESPACE;
import static org.jboss.osgi.repository.XRepository.SERVICE_NAMES;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;

import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.RepositoryStorageException;
import org.jboss.osgi.repository.RepositoryStorageFactory;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.core.FileBasedRepositoryStorage;
import org.jboss.osgi.repository.core.MavenRepository;
import org.jboss.osgi.repository.core.MavenRepository.ConfigurationPropertyProvider;
import org.jboss.osgi.repository.spi.AbstractPersistentRepository;
import org.jboss.osgi.repository.spi.AggregatingRepository;
import org.jboss.osgi.resolver.XResource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * The standalone {@link XRepository} service.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 31-Aug-2012
 */
class RepositoryService extends AbstractService<XRepository> {

    private final InjectedValue<BundleContext> injectedSystemContext = new InjectedValue<BundleContext>();
    private final InjectedValue<ServerEnvironment> injectedServerEnvironment = new InjectedValue<ServerEnvironment>();
    private ServiceRegistration<?> registration;
    private XRepository repository;

    static ServiceController<?> addService(final ServiceTarget target) {
        RepositoryService service = new RepositoryService();
        ServiceBuilder<?> builder = target.addService(OSGiConstants.REPOSITORY_SERVICE_NAME, service);
        builder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.injectedServerEnvironment);
        builder.addDependency(Services.FRAMEWORK_CREATE, BundleContext.class, service.injectedSystemContext);
        builder.setInitialMode(Mode.ON_DEMAND);
        return builder.install();
    }

    private RepositoryService() {
    }

    @Override
    public synchronized void start(StartContext startContext) throws StartException {

        // Create the {@link RepositoryStorageFactory}
        final ServerEnvironment serverenv = injectedServerEnvironment.getValue();
        RepositoryStorageFactory factory = new RepositoryStorageFactory() {
            @Override
            public RepositoryStorage create(XRepository repository) {
                File storageDir = new File(serverenv.getServerDataDir().getPath() + File.separator + "repository");
                return new FileBasedRepositoryStorage(repository, storageDir) {
                    @Override
                    public XResource addResource(XResource res) throws RepositoryStorageException {
                        // Do not add modules to repository storage
                        if (res.getCapabilities(MODULE_IDENTITY_NAMESPACE).isEmpty()) {
                            return super.addResource(res);
                        } else {
                            return res;
                        }
                    }
                };
            }
        };

        // Create the {@link ConfigurationPropertyProvider}
        final BundleContext syscontext = injectedSystemContext.getValue();
        ConfigurationPropertyProvider propProvider = new ConfigurationPropertyProvider() {
            @Override
            public String getProperty(String key, String defaultValue) {
                String value = syscontext.getProperty(key);
                return value != null ? value : defaultValue;
            }
        };

        AggregatingRepository aggregator = new AggregatingRepository();
        aggregator.addRepository(new ModuleIdentityRepository(serverenv));
        aggregator.addRepository(new MavenRepository(propProvider));
        repository = new AbstractPersistentRepository(factory, aggregator);

        // Register the top level repository
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION, repository.getName());
        registration = syscontext.registerService(SERVICE_NAMES, repository, props);
    }

    @Override
    public synchronized void stop(StopContext context) {
        if (registration != null) {
            registration.unregister();
        }
        repository = null;
    }

    @Override
    public synchronized XRepository getValue() throws IllegalStateException {
        return repository;
    }
}

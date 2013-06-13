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

import static org.jboss.osgi.repository.RepositoryMessages.MESSAGES;

import java.io.File;
import java.io.IOException;

import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.osgi.parser.SubsystemState;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.RepositoryStorageFactory;
import org.jboss.osgi.repository.XPersistentRepository;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.spi.AbstractPersistentRepository;
import org.jboss.osgi.repository.spi.AggregatingRepository;
import org.jboss.osgi.repository.spi.FileBasedRepositoryStorage;
import org.jboss.osgi.repository.spi.MavenDelegateRepository;
import org.jboss.osgi.repository.spi.MavenDelegateRepository.ConfigurationPropertyProvider;

/**
 * The standalone {@link XRepository} service.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 31-Aug-2012
 */
public final class RepositoryService extends AbstractService<XPersistentRepository> {

    public static final ServiceName SERVICE_NAME = OSGiConstants.SERVICE_BASE_NAME.append("XRepository");

    private final InjectedValue<ServerEnvironment> injectedServerEnvironment = new InjectedValue<ServerEnvironment>();
    private final InjectedValue<SubsystemState> injectedSubsystemState = new InjectedValue<SubsystemState>();
    private XPersistentRepository repository;

    public static ServiceController<?> addService(final ServiceTarget target) {
        RepositoryService service = new RepositoryService();
        ServiceBuilder<?> builder = target.addService(SERVICE_NAME, service);
        builder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.injectedServerEnvironment);
        builder.addDependency(OSGiConstants.SUBSYSTEM_STATE_SERVICE_NAME, SubsystemState.class, service.injectedSubsystemState);
        return builder.install();
    }

    private RepositoryService() {
    }

    @Override
    public synchronized void start(StartContext startContext) throws StartException {

        // Create the {@link ConfigurationPropertyProvider}
        final ConfigurationPropertyProvider propProvider = new ConfigurationPropertyProvider() {
            @Override
            public String getProperty(String key, String defaultValue) {
                SubsystemState subsystemState = injectedSubsystemState.getValue();
                String value = subsystemState.getProperties().get(key);
                return value != null ? value : defaultValue;
            }
        };

        // Create the {@link RepositoryStorageFactory}
        final ServerEnvironment serverenv = injectedServerEnvironment.getValue();
        RepositoryStorageFactory factory = new RepositoryStorageFactory() {
            @Override
            public RepositoryStorage create(XRepository repository) {
                File storageDir = getRepositoryStorageDir(propProvider, serverenv);
                return new FileBasedRepositoryStorage(repository, storageDir, propProvider);
            }
        };

        AggregatingRepository aggregator = new AggregatingRepository();
        aggregator.addRepository(new ModuleIdentityRepository(serverenv));
        aggregator.addRepository(new MavenDelegateRepository(propProvider));
        repository = new AbstractPersistentRepository(factory, aggregator);
    }

    private File getRepositoryStorageDir(ConfigurationPropertyProvider propProvider, ServerEnvironment serverenv) {
        String dirName = propProvider.getProperty(XRepository.PROPERTY_REPOSITORY_STORAGE_DIR, null);
        if (dirName == null) {
            try {
                File storageDir = new File(serverenv.getServerDataDir().getPath() + File.separator + "repository");
                dirName = storageDir.getCanonicalPath();
            } catch (IOException ex) {
                throw MESSAGES.cannotCreateRepositoryStorageArea(ex);
            }
        }
        return new File(dirName).getAbsoluteFile();
    }

    @Override
    public synchronized void stop(StopContext context) {
        repository = null;
    }

    @Override
    public synchronized XPersistentRepository getValue() throws IllegalStateException {
        return repository;
    }
}

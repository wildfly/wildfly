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

package org.jboss.as.host.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;

import java.io.File;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.persistence.ConfigurationFile;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;

/**
 * Configuration persister that can delegate to a domain or host persister depending what needs to be persisted.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class HostControllerConfigurationPersister implements ExtensibleConfigurationPersister {

    private final HostControllerEnvironment environment;
    private ExtensibleConfigurationPersister domainPersister;
    private final ExtensibleConfigurationPersister hostPersister;
    private Boolean slave;

    public HostControllerConfigurationPersister(final HostControllerEnvironment environment) {
        this.environment = environment;
        final File configDir = environment.getDomainConfigurationDir();
        final ConfigurationFile configurationFile = environment.getHostConfigurationFile();
        this.hostPersister = ConfigurationPersisterFactory.createHostXmlConfigurationPersister(configDir, configurationFile);
    }

    public void initializeDomainConfigurationPersister(boolean slave) {
        if (domainPersister != null) {
            throw new IllegalStateException("Configuration persister for domain model is already initialized");
        }

        final File configDir = environment.getDomainConfigurationDir();
        if (slave) {
            if (environment.isBackupDomainFiles() || environment.isUseCachedDc()) {
                domainPersister = ConfigurationPersisterFactory.createCachedRemoteDomainXmlConfigurationPersister(configDir);
            } else {
                domainPersister = ConfigurationPersisterFactory.createTransientDomainXmlConfigurationPersister();
            }
        } else {
            final ConfigurationFile configurationFile = environment.getDomainConfigurationFile();
            domainPersister = ConfigurationPersisterFactory.createDomainXmlConfigurationPersister(configDir, configurationFile);
        }

        this.slave = Boolean.valueOf(slave);
    }

    public boolean isSlave() {
        if (slave == null) {
            throw new IllegalStateException("Must call initializeDomainConfigurationPersister before checking for slave status");
        }
        return slave;
    }

    public ExtensibleConfigurationPersister getDomainPersister() {
        if (domainPersister == null) {
            throw new IllegalStateException("Must call initializeDomainConfigurationPersister before persisting the domain model");
        }
        return domainPersister;
    }

    public ExtensibleConfigurationPersister getHostPersister() {
        return hostPersister;
    }

    @Override
    public PersistenceResource store(ModelNode model, Set<PathAddress> affectedAddresses) throws ConfigurationPersistenceException {

        if (true) {
            System.out.println("==== HostControllerConfigurationPersister temporarily disabled see HCCP.store()");
            return new PersistenceResource() {

                @Override
                public void rollback() {
                }

                @Override
                public void commit() {
                }
            };
        }

        final PersistenceResource[] delegates = new PersistenceResource[2];
        for (PathAddress addr : affectedAddresses) {
            if (delegates[0] == null && addr.size() > 0 && HOST.equals(addr.getElement(0).getKey())) {
                ModelNode hostModel = new ModelNode();
                hostModel.get(HOST).set(model.get(HOST));
                delegates[0] = hostPersister.store(hostModel, affectedAddresses);
            } else if (delegates[1] == null && (addr.size() == 0 || !HOST.equals(addr.getElement(0).getKey()))) {
                delegates[1] = getDomainPersister().store(model, affectedAddresses);
            }

            if (delegates[0] != null && delegates[1] != null) {
                break;
            }
        }

        return new PersistenceResource() {
            @Override
            public void commit() {
                if (delegates[0] != null) {
                    delegates[0].commit();
                }
                if (delegates[1] != null) {
                    delegates[1].commit();
                }
            }

            @Override
            public void rollback() {
                if (delegates[0] != null) {
                    delegates[0].rollback();
                }
                if (delegates[1] != null) {
                    delegates[1].rollback();
                }
            }
        };
    }

    @Override
    public void marshallAsXml(ModelNode model, OutputStream output) throws ConfigurationPersistenceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ModelNode> load() throws ConfigurationPersistenceException {
        return hostPersister.load();
    }

    @Override
    public void successfulBoot() throws ConfigurationPersistenceException {
        hostPersister.successfulBoot();
        domainPersister.successfulBoot();
    }

    @Override
    public String snapshot() throws ConfigurationPersistenceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SnapshotInfo listSnapshots() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteSnapshot(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerSubsystemWriter(String name, XMLElementWriter<SubsystemMarshallingContext> writer) {
        domainPersister.registerSubsystemWriter(name, writer);
    }

    @Override
    public void registerSubsystemDeploymentWriter(String name, XMLElementWriter<SubsystemMarshallingContext> writer) {
        domainPersister.registerSubsystemDeploymentWriter(name, writer);
    }
}

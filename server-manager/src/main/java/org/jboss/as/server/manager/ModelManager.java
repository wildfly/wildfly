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

package org.jboss.as.server.manager;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLInputFactory;

import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.client.impl.HostUpdateApplierResponse;
import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.AbstractHostModelUpdate;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.HostModel;
import org.jboss.as.model.NewRepositoryContentUpdate;
import org.jboss.as.model.ServerElement;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLMapper;

/**
 * {@link ServerManager} component that is responsible for managing the ServerManager's
 * {@link HostModel} and its copy of the {@link DomainModel}.
 *
 * @author Brian Stansberry
 */
public class ModelManager {
    private static final Logger log = Logger.getLogger("org.jboss.as.server.manager");

    private final HostConfigurationPersister configPersister;
    private DomainModel domainModel;
    private volatile HostModel hostModel;

    private final StandardElementReaderRegistrar extensionRegistrar;
    private FileRepository repository;

    ModelManager(final ServerManagerEnvironment environment, final StandardElementReaderRegistrar extensionRegistrar) {
        this(new HostConfigurationPersisterImpl(environment.getDomainConfigurationDir()), extensionRegistrar);
    }

    ModelManager(final HostConfigurationPersister configPersister, final StandardElementReaderRegistrar extensionRegistrar) {

        assert configPersister != null : "configPersister is null";
        assert extensionRegistrar != null : "extensionRegistrar is null";
        this.configPersister = configPersister;
        this.extensionRegistrar = extensionRegistrar;
    }


    public void start() {
        getHostModel();
    }

    public HostModel getHostModel() {
        if (hostModel == null) {
            synchronized (configPersister) {
                if (hostModel == null) {
                    hostModel = parseHostXml();
                }
            }
        }
        return hostModel;
    }

    public DomainModel getDomainModel() {
        return domainModel;
    }

    void setDomainModel(final DomainModel model) {
        assert model != null : "model is null";
        this.domainModel = model;
    }

    void setFileRepository(final FileRepository repository) {
        this.repository = repository;
    }

    public List<ServerIdentity> applyDomainModelUpdate(AbstractDomainModelUpdate<?> update, boolean applyToDomain) throws UpdateFailedException {

        log.debugf("Applying %s to server manager model", update.getClass().getSimpleName());
        if (applyToDomain) {
            try {
                // Force a sync if our repository falls back to a remote
                if (update instanceof NewRepositoryContentUpdate && repository != null)
                    repository.getDeploymentRoot(((NewRepositoryContentUpdate) update).getHash());

                domainModel.update(update);
            }
            catch (UpdateFailedException e) {
                // TODO mark ourself in a state where we require
                // correction by the DC.
                throw e;
            }
        }


        List<String> serverNames = update.getAffectedServers(domainModel, getHostModel());
        log.debugf("Servers affected: %s", serverNames);

        if (serverNames.size() == 0) {
            return Collections.emptyList();
        }
        List<ServerIdentity> ids = new ArrayList<ServerIdentity>(serverNames.size());
        String hostName = hostModel.getName();
        for (String server : serverNames) {
            ServerElement se = hostModel.getServer(server);
            ids.add(new ServerIdentity(hostName, se.getServerGroup(), server));
        }
        return ids;
    }


    public List<HostUpdateApplierResponse> applyHostModelUpdates(List<AbstractHostModelUpdate<?>> updates) {


        List<HostUpdateApplierResponse> result = new ArrayList<HostUpdateApplierResponse>(updates.size());

        // First we apply updates to our local model copy
        boolean ok = true;
        List<AbstractHostModelUpdate<?>> rollbacks = new ArrayList<AbstractHostModelUpdate<?>>();
        for (AbstractHostModelUpdate<?> update : updates) {
            if (ok) {
                try {
                    AbstractHostModelUpdate<?> rollback = update.getCompensatingUpdate(hostModel);
                    hostModel.update(update);
                    // Add the rollback after success so we don't rollback
                    // the failed update -- which should not have changed anything
                    rollbacks.add(0, rollback);
                    // Stick in a placeholder result that will survive if
                    // a host update faiure triggers a rollback or will get replaced with
                    // the final result if we apply to servers
                    result.add(new HostUpdateApplierResponse(false));
                }
                catch (UpdateFailedException e) {
                    ok = false;
                    result.add(new HostUpdateApplierResponse(e));
                }
            } else {
                // Add a cancellation response
                result.add(new HostUpdateApplierResponse(true));
            }
        }

        if (!ok) {
            // Apply compensating updates to fix our local model
            for (int i = 0; i < rollbacks.size(); i++) {
                AbstractHostModelUpdate<?> rollback = rollbacks.get(i);
                try {
                    hostModel.update(rollback);
                }
                catch (UpdateFailedException e) {
                    // TODO uh oh. Reload from the file?
                }
            }
        }
        else {
            // Persist model
            configPersister.persistConfiguration(hostModel);

            for (AbstractHostModelUpdate<?> update : updates) {
                result.add(new HostUpdateApplierResponse(getAffectedServers(update)));
            }
        }

        return result;
    }

    public List<ServerIdentity> applyHostModelUpdate(AbstractHostModelUpdate<?> update) throws UpdateFailedException {

        List<HostUpdateApplierResponse> rsps = applyHostModelUpdates(Collections.<AbstractHostModelUpdate<?>>singletonList(update));
        HostUpdateApplierResponse rsp = rsps.get(0);
        if (rsp.getHostFailure() != null) {
            throw rsp.getHostFailure();
        }
        else {
            return rsp.getServers();
        }
    }


    private List<ServerIdentity> getAffectedServers(AbstractHostModelUpdate<?> update) {
        List<String> serverNames = update.getAffectedServers(hostModel);
        if (serverNames.size() == 0) {
            return Collections.emptyList();
        }
        List<ServerIdentity> ids = new ArrayList<ServerIdentity>(serverNames.size());
        String hostName = hostModel.getName();
        for (String server : serverNames) {
            ServerElement se = hostModel.getServer(server);
            ids.add(new ServerIdentity(hostName, se.getServerGroup(), server));
        }
        return ids;
    }

    private HostModel parseHostXml() {

        try {
            final List<AbstractHostModelUpdate<?>> hostUpdates = new ArrayList<AbstractHostModelUpdate<?>>();
            final XMLMapper mapper = XMLMapper.Factory.create();
            extensionRegistrar.registerStandardHostReaders(mapper);
            mapper.parseDocument(hostUpdates, XMLInputFactory.newInstance().createXMLStreamReader(new BufferedReader(configPersister.getConfigurationReader())));
            final HostModel hostModel = new HostModel();
            for(final AbstractHostModelUpdate<?> update : hostUpdates) {
                hostModel.update(update);
            }
            return hostModel;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Caught exception during processing of host.xml", e);
        }
    }
}

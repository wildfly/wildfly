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
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.stream.XMLInputFactory;

import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.AbstractHostModelUpdate;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.HostModel;
import org.jboss.as.model.ServerElement;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.staxmapper.XMLMapper;

/**
 * {@link ServerManager} component that is responsible for managing the ServerManager's
 * {@link HostModel} and its copy of the {@link DomainModel}.
 *
 * @author Brian Stansberry
 */
public class ModelManager {

    private final File hostXml;
    private DomainModel domainModel;
    private boolean localDomainController;
    private volatile HostModel hostModel;

    private final StandardElementReaderRegistrar extensionRegistrar;

    ModelManager(final ServerManagerEnvironment environment, final StandardElementReaderRegistrar extensionRegistrar) {
        assert environment != null : "environment is null";
        assert extensionRegistrar != null : "extensionRegistrar is null";
        this.hostXml = new File(environment.getDomainConfigurationDir(), "host.xml");
        this.extensionRegistrar = extensionRegistrar;
    }


    public void start() {
        getHostModel();
    }

    public HostModel getHostModel() {
        if (hostModel == null) {
            synchronized (hostXml) {
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

    void setDomainModel(final DomainModel model, final boolean localDomainController) {
        assert model != null : "model is null";
        this.domainModel = model;
        this.localDomainController = localDomainController;
    }

    public List<ServerIdentity> applyDomainModelUpdate(AbstractDomainModelUpdate<?> update) throws UpdateFailedException {

        // Only apply the update to the domainModel if we don't have a local
        // DC that's already done it
        if (!localDomainController) {
            domainModel.update(update);
        }

        List<String> serverNames = update.getAffectedServers(domainModel, getHostModel());
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

    public List<ServerIdentity> applyHostModelUpdate(AbstractHostModelUpdate<?> update) throws UpdateFailedException {

        getHostModel().update(update);

        // FIXME persist host.xml

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

        if (!hostXml.exists()) {
            throw new IllegalStateException("File " + hostXml.getAbsolutePath() + " does not exist.");
        }
        else if (! hostXml.canWrite()) {
            throw new IllegalStateException("File " + hostXml.getAbsolutePath() + " is not writeable.");
        }

        try {
            final List<AbstractHostModelUpdate<?>> hostUpdates = new ArrayList<AbstractHostModelUpdate<?>>();
            final XMLMapper mapper = XMLMapper.Factory.create();
            extensionRegistrar.registerStandardHostReaders(mapper);
            mapper.parseDocument(hostUpdates, XMLInputFactory.newInstance().createXMLStreamReader(new BufferedReader(new FileReader(this.hostXml))));
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

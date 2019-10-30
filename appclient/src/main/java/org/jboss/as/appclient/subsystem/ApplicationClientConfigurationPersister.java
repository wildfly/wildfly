/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.appclient.subsystem;

import java.io.File;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.persistence.XmlConfigurationPersister;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;

/**
 * Application client configuration.
 *
 * This configuration is currently hard coded to a minimal set of services required for app client boot
 *
 * @author Stuart Douglas
 */
public class ApplicationClientConfigurationPersister extends XmlConfigurationPersister {

    /**
     * The absolute path to the deployment archive
     */
    private final String filePath;

    /**
     * The app client was specified with the myear.ear#appclient.jar syntax, if any
     */
    private final String deploymentName;

    /**
     * Any additional parameters to pass to the application client
     */
    private final List<String> parameters;

    /**
     * The URL of the AS7 instance to connect to
     */
    private final String hostUrl;

    /**
     * This URL of an ejb-client.properties file
     */
    private final String propertiesFileURL;

    public ApplicationClientConfigurationPersister(final String filePath, final String deploymentName, final String hostUrl, final String propertiesFileUrl, final List<String> parameters, final File configFile, final QName element, final XMLElementReader<List<ModelNode>> xmlParser) {
        super(configFile, element, xmlParser, null);
        this.filePath = filePath;
        this.deploymentName = deploymentName;
        this.hostUrl = hostUrl;
        this.parameters = parameters;
        this.propertiesFileURL = propertiesFileUrl;
    }


    @Override
    public PersistenceResource store(final ModelNode model, final Set<PathAddress> affectedAddresses) throws ConfigurationPersistenceException {
        return new PersistenceResource() {
            @Override
            public void commit() {

            }

            @Override
            public void rollback() {

            }
        };
    }

    @Override
    public void marshallAsXml(final ModelNode model, final OutputStream output) throws ConfigurationPersistenceException {

    }

    @Override
    public List<ModelNode> load() throws ConfigurationPersistenceException {
        List<ModelNode> nodes = super.load();
        return AppClientServerConfiguration.serverConfiguration(filePath, deploymentName, hostUrl, propertiesFileURL, parameters, nodes);
    }

    @Override
    public void successfulBoot() throws ConfigurationPersistenceException {

    }

    @Override
    public String snapshot() throws ConfigurationPersistenceException {
        return null;
    }

    @Override
    public SnapshotInfo listSnapshots() {
        return null;
    }

    @Override
    public void deleteSnapshot(final String name) {

    }

    @Override
    public void registerSubsystemWriter(final String name, final XMLElementWriter<SubsystemMarshallingContext> writer) {

    }
}

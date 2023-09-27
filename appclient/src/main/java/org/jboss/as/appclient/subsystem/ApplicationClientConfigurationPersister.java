/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

    // TODO once WFCORE-6362 is integrated remove this method
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

    // TODO once WFCORE-6362 is integrated remove this method
    public void registerSubsystemWriter(final String name, final XMLElementWriter<SubsystemMarshallingContext> writer) {

    }
}

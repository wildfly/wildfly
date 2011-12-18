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

package org.jboss.as.controller.persistence;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLMapper;

import static org.jboss.as.controller.ControllerLogger.ROOT_LOGGER;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;

/**
 * Abstract superclass for {@link ExtensibleConfigurationPersister} implementations.
 *
 * @author Brian Stansberry
 */
public abstract class AbstractConfigurationPersister implements ExtensibleConfigurationPersister {

    private final XMLElementWriter<ModelMarshallingContext> rootDeparser;
    private final Map<String, XMLElementWriter<SubsystemMarshallingContext>> subsystemWriters = new HashMap<String, XMLElementWriter<SubsystemMarshallingContext>>();
    private final Map<String, XMLElementWriter<SubsystemMarshallingContext>> subsystemDeploymentWriters = new HashMap<String, XMLElementWriter<SubsystemMarshallingContext>>();

    /**
     * Construct a new instance.
     *
     * @param rootDeparser the root model deparser
     */
    public AbstractConfigurationPersister(final XMLElementWriter<ModelMarshallingContext> rootDeparser) {
        this.rootDeparser = rootDeparser;
    }

    @Override
    public void registerSubsystemWriter(String name, XMLElementWriter<SubsystemMarshallingContext> deparser) {
        synchronized (subsystemWriters) {
            subsystemWriters.put(name, deparser);
        }
    }

    @Override
    public void unregisterSubsystemWriter(String name) {
        synchronized (subsystemWriters) {
            subsystemWriters.remove(name);
        }
    }

    @Override
    public void registerSubsystemDeploymentWriter(String name, XMLElementWriter<SubsystemMarshallingContext> deparser) {
        synchronized (subsystemDeploymentWriters) {
            subsystemDeploymentWriters.put(name, deparser);
        }
    }

    @Override
    public void unregisterSubsystemDeploymentWriter(String name) {
        synchronized (subsystemDeploymentWriters) {
            subsystemDeploymentWriters.remove(name);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void marshallAsXml(final ModelNode model, final OutputStream output) throws ConfigurationPersistenceException {
        final XMLMapper mapper = XMLMapper.Factory.create();
        try {
            XMLStreamWriter streamWriter = null;
            try {
                streamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(output);
                ModelMarshallingContext extensibleModel = new ModelMarshallingContext() {

                    @Override
                    public ModelNode getModelNode() {
                        return model;
                    }

                    @Override
                    public XMLElementWriter<SubsystemMarshallingContext> getSubsystemWriter(String extensionName) {
                        synchronized (subsystemWriters) {
                            return subsystemWriters.get(extensionName);
                        }
                    }

                    @Override
                    public XMLElementWriter<SubsystemMarshallingContext> getSubsystemDeploymentWriter(String extensionName) {
                        synchronized (subsystemDeploymentWriters) {
                            return subsystemDeploymentWriters.get(extensionName);
                        }
                    }
                };
                mapper.deparseDocument(rootDeparser, extensibleModel, streamWriter);
                streamWriter.close();
            } finally {
                safeClose(streamWriter);
            }
        } catch (Exception e) {
            throw MESSAGES.failedToWriteConfiguration(e);
        }
    }

    @Override
    public void successfulBoot() throws ConfigurationPersistenceException {
    }

    @Override
    public String snapshot() throws ConfigurationPersistenceException{
        return null;
    }

    @Override
    public SnapshotInfo listSnapshots() {
        return NULL_SNAPSHOT_INFO;
    }

    @Override
    public void deleteSnapshot(String name) {
    }

    private static void safeClose(final XMLStreamWriter streamWriter) {
        if (streamWriter != null) try {
            streamWriter.close();
        } catch (Throwable t) {
            ROOT_LOGGER.failedToCloseResource(t, streamWriter);
        }
    }
}

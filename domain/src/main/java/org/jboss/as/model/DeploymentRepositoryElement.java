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

package org.jboss.as.model;

import java.io.File;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.deployment.filesystem.FileSystemDeploymentService;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Configuration for a location where deployment content is stored.
 *
 * @author Brian Stansberry
 */
public class DeploymentRepositoryElement extends AbstractModelElement<DeploymentRepositoryElement> implements ServiceActivator {

    private static final long serialVersionUID = -8564235225752540162L;

    public static final String DEFAULT_STANDALONE_PATH = "standalone/deployments";

    private final String path;
    private int interval = 0;
    private boolean enabled = true;

    /**
     * @param location
     */
    public DeploymentRepositoryElement(String path, int interval, boolean enabled) {
        super();
        if (path == null)
            throw new IllegalArgumentException("path is null");
        this.path = path;
        this.interval = interval;
        this.enabled = enabled;
    }

    /**
     * @param reader
     * @throws XMLStreamException
     */
    public DeploymentRepositoryElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        this(reader, DEFAULT_STANDALONE_PATH);
    }

    public DeploymentRepositoryElement(XMLExtendedStreamReader reader, String defaultPath) throws XMLStreamException {
        super();
        // Handle attributes
        String path = defaultPath;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case PATH: {
                        path = value;
                        break;
                    }
                    case SCAN_INTERVAL: {
                        interval = Integer.valueOf(value);
                        break;
                    }
                    case SCAN_ENABLED: {
                        enabled = Boolean.parseBoolean(value);
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        this.path = path;
        // Handle elements
        ParseUtils.requireNoContent(reader);
    }

    /**
     * Gets the filesystem path of the root of the repository. Either absolute
     * or relative to the current working directory.
     *
     * @return the path. Will not be <code>null</code>
     */
    public String getPath() {
        return path;
    }

    public int getScanInterval() {
        return interval;
    }

    public boolean isScanEnabled() {
        return enabled;
    }

    @Override
    private long elementHash() {
        long hash = path.hashCode() & 0xffffffffL;
        hash = Long.rotateLeft(hash, 1) ^ interval & 0xffffffffL;
        hash = Long.rotateLeft(hash, 1) ^ Boolean.valueOf(enabled).hashCode() & 0xffffffffL;
        return hash;
    }

    @Override
    protected Class<DeploymentRepositoryElement> getElementClass() {
        return DeploymentRepositoryElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.PATH.getLocalName(), path);
        if (interval != 0)
            streamWriter.writeAttribute(Attribute.SCAN_INTERVAL.getLocalName(), String.valueOf(interval));
        if (!enabled)
            streamWriter.writeAttribute(Attribute.SCAN_ENABLED.getLocalName(), "false");
        streamWriter.writeEndElement();
    }

    @Override
    public void activate(ServiceActivatorContext serviceActivatorContext) {
        String absolutePath = getAbsolutePath(path);
        FileSystemDeploymentService.addService(serviceActivatorContext.getBatchBuilder(), absolutePath, interval, enabled);
    }

    private String getAbsolutePath(String path) {
        if (File.separatorChar == '/') {
            if (path.startsWith(File.separator)) {
                return path;
            }
        }
        else if (path.indexOf(":\\") == 1) {
            return path;
        }
        // TODO. Yuck. Better would be to use ServerEnvironment
        String jbossHome = System.getProperty("jboss.home.dir");
        return jbossHome.endsWith(File.separator) ? jbossHome + path : jbossHome + File.separatorChar + path;
    }

}

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

import javax.xml.stream.XMLStreamException;

import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Configuration for a location where deployment content is stored.
 *
 * @author Brian Stansberry
 */
public class DeploymentRepositoryElement extends AbstractModelElement<DeploymentRepositoryElement> {

    private static final long serialVersionUID = -8564235225752540162L;

    public static final String DEFAULT_STANDALONE_PATH = "standalone/deployments";

    private final String path;
    private int interval = 0;
    private boolean enabled = true;

    /**
     * Creates a new {@code DeploymentRepsoitoryElement}
     *
     * @param path the repository path
     */
    public DeploymentRepositoryElement(String path) {
        this.path = path;
    }

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

    void setInterval(int interval) {
        this.interval = interval;
    }

    public boolean isScanEnabled() {
        return enabled;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
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


}

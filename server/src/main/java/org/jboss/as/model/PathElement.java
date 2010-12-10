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

import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A named domain model filesystem path.
 *
 * @author Emanuel Muckenhuber
 */
public class PathElement extends AbstractModelElement<PathElement> {

    /** The restricted path names. */
    static final Set<String> RESTRICTED = new HashSet<String>(10);

    static {
        // Define the restricted path names.
        RESTRICTED.add("jboss.home");
        RESTRICTED.add("jboss.home.dir");
        RESTRICTED.add("user.home");
        RESTRICTED.add("user.dir");
        RESTRICTED.add("java.home");
        RESTRICTED.add("jboss.server.base.dir");
        RESTRICTED.add("jboss.server.data.dir");
        RESTRICTED.add("jboss.server.log.dir");
        RESTRICTED.add("jboss.server.tmp.dir");
        // NOTE we actually don't create services for the following
        // however the names remain restricted for use in the configuration
        RESTRICTED.add("jboss.modules.dir");
        RESTRICTED.add("jboss.server.deploy.dir");
        RESTRICTED.add("jboss.domain.servers.dir");
    }

    private static final long serialVersionUID = 5502158760122357895L;
    private final String name;
    private String path;
    private String relativeTo;

    protected PathElement(String name) {
        if(name == null) {
            throw new IllegalArgumentException("null name");
        }
        if(RESTRICTED.contains(name)) {
            // TODO error code
            throw new IllegalArgumentException("Illegal path name. Cannot be one of the standard " +
                    "fixed paths defined by the system.");
        }
        this.name = name;
    }

    /** {@inheritDoc} */
    protected Class<PathElement> getElementClass() {
        return PathElement.class;
    }

    /**
     * Get the element path name.
     *
     * @return the name the path name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the path.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    void setPath(String path) {
        this.path = path;
    }

    /**
     * Get the relative-to.
     *
     * @return the relativeTo
     */
    public String getRelativeTo() {
        return relativeTo;
    }

    void setRelativeTo(String relativeTo) {
        this.relativeTo = relativeTo;
    }

    /**
     * Check whether this path is absolute or relative to
     * another {@code PathElement}
     *
     * @return <code>true</code> if this element is absolute, <code>false</code> otherwise
     */
    public boolean isAbsolutePath() {
        final String relativeTo = this.relativeTo;
        return relativeTo == null;
    }

    /**
     * Check if the path is specified.
     *
     * @return <code>true</code> if the path element is specified, <code>false</code> otherwise
     */
    public boolean isSpecified() {
        final String path = this.path;
        return path != null;
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        if(path != null) streamWriter.writeAttribute(Attribute.PATH.getLocalName(), path);
        if(relativeTo != null) streamWriter.writeAttribute(Attribute.RELATIVE_TO.getLocalName(), relativeTo);
        streamWriter.writeEndElement();
    }

}

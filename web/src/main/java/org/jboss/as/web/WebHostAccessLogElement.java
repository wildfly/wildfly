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
package org.jboss.as.web;

import java.io.Serializable;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLContentWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The virtual server access log configuration.
 *
 * @author Emanuel Muckenhuber
 */
public class WebHostAccessLogElement extends AbstractModelElement<WebHostAccessLogElement> {

    /** The serialVersionUID */
    private static final long serialVersionUID = 7372525549178383501L;
    private LogDirectory directory;
    private String pattern;
    private String prefix;
    private Boolean resolveHosts;
    private Boolean extended;
    private Boolean rotate;

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public LogDirectory getDirectory() {
        return directory;
    }

    public void setDirectory(LogDirectory directory) {
        this.directory = directory;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Boolean isExtended() {
        return extended;
    }

    public void setExtended(Boolean extended) {
        this.extended = extended;
    }

    public Boolean isResolveHosts() {
        return resolveHosts;
    }

    public void setResolveHosts(Boolean resolveHosts) {
        this.resolveHosts = resolveHosts;
    }

    public Boolean isRotate() {
        return rotate;
    }

    public void setRotate(Boolean rotate) {
        this.rotate = rotate;
    }

    /** {@inheritDoc} */
    protected Class<WebHostAccessLogElement> getElementClass() {
        return WebHostAccessLogElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        if (directory != null && directory.isEmpty()) {
            streamWriter.writeEmptyElement(Element.DIRECTORY.getLocalName());
            directory.writeContent(streamWriter);
        }
        if (pattern != null) {
            streamWriter.writeAttribute(Attribute.PATTERN.getLocalName(), pattern);
        }
        if (prefix != null) {
            streamWriter.writeAttribute(Attribute.PREFIX.getLocalName(), prefix);
        }
        if(extended != null) {
            streamWriter.writeAttribute(Attribute.EXTENDED.getLocalName(), String.valueOf(extended));
        }
        if(resolveHosts != null) {
            streamWriter.writeAttribute(Attribute.RESOLVE_HOSTS.getLocalName(), String.valueOf(resolveHosts));
        }
        if(rotate != null) {
            streamWriter.writeAttribute(Attribute.ROTATE.getLocalName(), String.valueOf(rotate));
        }
        streamWriter.writeEndElement();
    }

    public static class LogDirectory implements Serializable, Cloneable, XMLContentWriter{
        private static final long serialVersionUID = -957540463350589398L;
        private String relativeTo;
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getRelativeTo() {
            return relativeTo;
        }

        public void setRelativeTo(String relativeTo) {
            this.relativeTo = relativeTo;
        }

        boolean isEmpty() {
            return relativeTo == null && path == null;
        }

        /** {@inheritDoc} */
        public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
            if(relativeTo != null) {
                streamWriter.writeAttribute(Attribute.RELATIVE_TO.getLocalName(), relativeTo);
            }
            if(path != null) {
                streamWriter.writeAttribute(Attribute.PATH.getLocalName(), path);
            }
        }
    }

}

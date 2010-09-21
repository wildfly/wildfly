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

import java.util.Collection;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.Extension;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLMapper;

/**
 * A model extension element.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ExtensionElement extends AbstractModelElement<ExtensionElement> {

    private static final long serialVersionUID = -90177370272205647L;

    private final String module;

    /**
     * Construct a new instance.
     *
     * @param module the module identifier of the extension
     */
    public ExtensionElement(final String module) {
        this.module = module;
    }

    public ExtensionElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // Handle attributes
        this.module = readStringAttributeElement(reader, Attribute.MODULE.getLocalName());

        // Register element handlers for this extension
        try {
            final XMLMapper xmlMapper = reader.getXMLMapper();
            for (Extension extension : Module.loadService(module, Extension.class)) {
                extension.registerElementHandlers(xmlMapper);
            }
        } catch (ModuleLoadException e) {
            throw new XMLStreamException("Failed to load module", e);
        }
    }

    /**
     * Gets the module identifier of the extension
     *
     * @return the module identifier
     */
    public String getModule() {
        return module;
    }

    /** {@inheritDoc} */
    public long elementHash() {
        final String module = this.module;
        long hc = module.hashCode() & 0xFFFFFFFF;
        return hc;
    }

    /** {@inheritDoc} */
    protected void appendDifference(final Collection<AbstractModelUpdate<ExtensionElement>> target, final ExtensionElement other) {
        // no mutable state
    }

    /** {@inheritDoc} */
    protected Class<ExtensionElement> getElementClass() {
        return ExtensionElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.MODULE.getLocalName(), module);
        streamWriter.writeEndElement();
    }
}

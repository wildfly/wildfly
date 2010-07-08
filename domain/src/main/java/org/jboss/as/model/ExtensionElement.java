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
import javax.xml.stream.XMLStreamWriter;

/**
 * A model extension element.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ExtensionElement extends AbstractModelElement<ExtensionElement> {

    private static final long serialVersionUID = -90177370272205647L;

    private final String prefix;
    private final String module;

    /**
     * Construct a new instance.
     *
     * @param prefix the extension prefix, if any
     * @param module the module identifier of the subsystem
     */
    public ExtensionElement(final String prefix, final String module) {
        if (module == null) {
            throw new IllegalArgumentException("module is null");
        }
        this.prefix = prefix;
        this.module = module;
    }

    /** {@inheritDoc} */
    public long elementHash() {
        final String module = this.module;
        final String prefix = this.prefix;
        long hc = (prefix == null ? 0 : prefix.hashCode() & 0xFFFFFFFFL << 32L) | module.hashCode() & 0xFFFFFFFF;
        return hc;
    }

    /** {@inheritDoc} */
    public boolean isSameElement(final ExtensionElement other) {
        return (prefix == null ? other.prefix == null : prefix.equals(other.prefix)) && module.equals(other.module);
    }

    /** {@inheritDoc} */
    protected void appendDifference(final Collection<AbstractModelUpdate<ExtensionElement>> target, final ExtensionElement other) {
        // no mutable state
    }

    /** {@inheritDoc} */
    protected Class<ExtensionElement> getElementClass() {
        return ExtensionElement.class;
    }

    public void writeContent(final XMLStreamWriter streamWriter) throws XMLStreamException {
        final String prefix = this.prefix;
        if (prefix != null) streamWriter.writeAttribute("prefix", prefix);
        streamWriter.writeAttribute("module", module);
        streamWriter.writeEndElement();
    }
}

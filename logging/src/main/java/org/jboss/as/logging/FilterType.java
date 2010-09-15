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

package org.jboss.as.logging;

import org.jboss.logmanager.filters.AcceptAllFilter;
import org.jboss.logmanager.filters.DenyAllFilter;
import org.jboss.staxmapper.XMLContentWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

import java.util.logging.Filter;

public abstract class FilterType implements XMLContentWriter {

    /**
     * Create the logging filter instance.
     *
     * @return the filter instance
     */
    public abstract Filter createFilterInstance();

    /**
     * Write the content for this filter element, including the start element name.
     *
     * @param writer the stream writer
     * @throws XMLStreamException if an error occurs
     */
    public abstract void writeContent(final XMLExtendedStreamWriter writer) throws XMLStreamException;

    public static final FilterType ACCEPT = new BooleanFilterType(AcceptAllFilter.getInstance(), Element.ACCEPT.getLocalName());
    public static final FilterType DENY = new BooleanFilterType(DenyAllFilter.getInstance(), Element.DENY.getLocalName());

    private static class BooleanFilterType extends FilterType {

        private final Filter filter;
        private final String localName;

        public BooleanFilterType(final Filter filter, final String localName) {
            this.filter = filter;
            this.localName = localName;
        }

        public Filter createFilterInstance() {
            return filter;
        }

        public void writeContent(final XMLExtendedStreamWriter writer) throws XMLStreamException {
            writer.writeEmptyElement(localName);
        }
    }
}

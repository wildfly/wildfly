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

package org.jboss.as.ejb3.cache;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaDataParser;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Parser for <code>urn:ejb-cache</code> namespace. The <code>urn:ejb-cache</code> namespace elements
 * can be used to configure cache names for EJBs.
 *
 * @author Jaikiran Pai
 * @author Stuart Douglas
 */
public class EJBBoundCacheParser extends AbstractEJBBoundMetaDataParser<EJBBoundCacheMetaData> {

    public static final String NAMESPACE_URI = "urn:ejb-cache:1.0";

    private static final String ROOT_ELEMENT_CACHE = "cache";
    private static final String CACHE_REF = "cache-ref";

    @Override
    public EJBBoundCacheMetaData parse(final XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        final String element = reader.getLocalName();
        // we only parse <cache> (root) element
        if (!ROOT_ELEMENT_CACHE.equals(element)) {
            throw unexpectedElement(reader);
        }
        final EJBBoundCacheMetaData ejbBoundCacheMetaData = new EJBBoundCacheMetaData();
        this.processElements(ejbBoundCacheMetaData, reader, propertyReplacer);
        return ejbBoundCacheMetaData;
    }

    @Override
    protected void processElement(final EJBBoundCacheMetaData cacheMetaData, final XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        final String namespaceURI = reader.getNamespaceURI();
        final String elementName = reader.getLocalName();
        // if it doesn't belong to our namespace then let the super handle this
        if (!NAMESPACE_URI.equals(namespaceURI)) {
            super.processElement(cacheMetaData, reader, propertyReplacer);
            return;
        }
        if (CACHE_REF.equals(elementName)) {
            final String cacheName = getElementText(reader, propertyReplacer);
            // set the cache name in the metadata
            cacheMetaData.setCacheName(cacheName);
        } else {
            throw unexpectedElement(reader);
        }
    }
}

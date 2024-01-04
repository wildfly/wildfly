/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.cache;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaDataParser;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Parser for <code>urn:ejb-cache</code> namespace. The <code>urn:ejb-cache</code> namespace elements
 * can be used to configure cache names for Jakarta Enterprise Beans.
 *
 * @author Jaikiran Pai
 * @author Stuart Douglas
 */
public class EJBBoundCacheParser extends AbstractEJBBoundMetaDataParser<EJBBoundCacheMetaData> {

    public static final String NAMESPACE_URI_1_0 = "urn:ejb-cache:1.0";
    public static final String NAMESPACE_URI_2_0 = "urn:ejb-cache:2.0";


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
        if (!NAMESPACE_URI_1_0.equals(namespaceURI) && !NAMESPACE_URI_2_0.equals(namespaceURI)) {
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

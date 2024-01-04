/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.pool;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaDataParser;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Parser for <code>urn:ejb-pool</code> namespace. The <code>urn:ejb-pool</code> namespace elements
 * can be used to configure pool names for Jakarta Enterprise Beans.
 *
 * @author Jaikiran Pai
 */
public class EJBBoundPoolParser extends AbstractEJBBoundMetaDataParser<EJBBoundPoolMetaData> {

    public static final String NAMESPACE_URI_1_0 = "urn:ejb-pool:1.0";
    public static final String NAMESPACE_URI_2_0 = "urn:ejb-pool:2.0";

    private static final String ROOT_ELEMENT_POOL = "pool";
    private static final String ELEMENT_BEAN_INSTANCE_POOL_REF = "bean-instance-pool-ref";

    @Override
    public EJBBoundPoolMetaData parse(final XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        final String element = reader.getLocalName();
        // we only parse <pool> (root) element
        if (!ROOT_ELEMENT_POOL.equals(element)) {
            throw unexpectedElement(reader);
        }
        final EJBBoundPoolMetaData ejbBoundPoolMetaData = new EJBBoundPoolMetaData();
        this.processElements(ejbBoundPoolMetaData, reader, propertyReplacer);
        return ejbBoundPoolMetaData;
    }

    @Override
    protected void processElement(final EJBBoundPoolMetaData poolMetaData, final XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        final String namespaceURI = reader.getNamespaceURI();
        final String elementName = reader.getLocalName();
        // if it doesn't belong to our namespace then let the super handle this
        if (!NAMESPACE_URI_1_0.equals(namespaceURI) && !NAMESPACE_URI_2_0.equals(namespaceURI)) {
            super.processElement(poolMetaData, reader, propertyReplacer);
            return;
        }
        if (ELEMENT_BEAN_INSTANCE_POOL_REF.equals(elementName)) {
            final String poolName = getElementText(reader, propertyReplacer);
            // set the pool name in the metadata
            poolMetaData.setPoolName(poolName);
        } else {
            throw unexpectedElement(reader);
        }
    }
}

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

package org.jboss.as.ejb3.pool;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaDataParser;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Parser for <code>urn:ejb-pool</code> namespace. The <code>urn:ejb-pool</code> namespace elements
 * can be used to configure pool names for EJBs.
 *
 * @author Jaikiran Pai
 */
public class EJBBoundPoolParser extends AbstractEJBBoundMetaDataParser<EJBBoundPoolMetaData> {

    public static final String NAMESPACE_URI = "urn:ejb-pool:1.0";

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
        if (!NAMESPACE_URI.equals(namespaceURI)) {
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

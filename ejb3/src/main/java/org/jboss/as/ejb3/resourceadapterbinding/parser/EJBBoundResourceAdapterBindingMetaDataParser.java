/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.resourceadapterbinding.parser;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.ejb3.resourceadapterbinding.metadata.EJBBoundResourceAdapterBindingMetaData;
import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaDataParser;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Parser for EJBBoundSecurityMetaData components.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class EJBBoundResourceAdapterBindingMetaDataParser extends AbstractEJBBoundMetaDataParser<EJBBoundResourceAdapterBindingMetaData> {

    public static final String LEGACY_NAMESPACE_URI = "urn:resource-adapter-binding";
    public static final String NAMESPACE_URI_1_0 = "urn:resource-adapter-binding:1.0";
    public static final String NAMESPACE_URI_2_0 = "urn:resource-adapter-binding:2.0";
    public static final EJBBoundResourceAdapterBindingMetaDataParser INSTANCE = new EJBBoundResourceAdapterBindingMetaDataParser();

    private EJBBoundResourceAdapterBindingMetaDataParser() {

    }

    @Override
    public EJBBoundResourceAdapterBindingMetaData parse(XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        EJBBoundResourceAdapterBindingMetaData metaData = new EJBBoundResourceAdapterBindingMetaData();
        processElements(metaData, reader, propertyReplacer);
        return metaData;
    }

    @Override
    protected void processElement(EJBBoundResourceAdapterBindingMetaData metaData, XMLStreamReader reader,  final PropertyReplacer propertyReplacer) throws XMLStreamException {
        final String namespaceURI = reader.getNamespaceURI();
        final String localName = reader.getLocalName();
        if (LEGACY_NAMESPACE_URI.equals(namespaceURI) ||
                NAMESPACE_URI_2_0.equals(namespaceURI) ||
                NAMESPACE_URI_1_0.equals(namespaceURI)) {
            if ("resource-adapter-name".equals(localName)) {
                metaData.setResourceAdapterName(getElementText(reader, propertyReplacer));
            } else {
                throw unexpectedElement(reader);
            }
        } else {
            super.processElement(metaData, reader, propertyReplacer);
        }
    }

}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.security.parser;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.ejb3.security.metadata.EJBBoundSecurityMetaData;
import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaDataParser;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Parser for EJBBoundSecurityMetaData components.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class EJBBoundSecurityMetaDataParser extends AbstractEJBBoundMetaDataParser<EJBBoundSecurityMetaData> {

    public static final String LEGACY_NAMESPACE_URI = "urn:security";
    public static final String NAMESPACE_URI_1_0 = "urn:security:1.0";

    public static final EJBBoundSecurityMetaDataParser INSTANCE = new EJBBoundSecurityMetaDataParser();

    protected EJBBoundSecurityMetaDataParser() {

    }

    @Override
    public EJBBoundSecurityMetaData parse(XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        EJBBoundSecurityMetaData metaData = new EJBBoundSecurityMetaData();
        processElements(metaData, reader, propertyReplacer);
        return metaData;
    }

    @Override
    protected void processElement(EJBBoundSecurityMetaData metaData, XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        if (reader.getNamespaceURI().equals(LEGACY_NAMESPACE_URI) ||
                reader.getNamespaceURI().equals(NAMESPACE_URI_1_0)) {
            final String localName = reader.getLocalName();
            if (localName.equals("security-domain"))
                metaData.setSecurityDomain(getElementText(reader, propertyReplacer));
            else if (localName.equals("run-as-principal"))
                metaData.setRunAsPrincipal(getElementText(reader, propertyReplacer));
            else
                throw unexpectedElement(reader);
        } else {
            super.processElement(metaData, reader, propertyReplacer);
        }
    }

}

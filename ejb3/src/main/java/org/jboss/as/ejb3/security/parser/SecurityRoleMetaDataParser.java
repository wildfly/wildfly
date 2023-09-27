/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.security.parser;

import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.metadata.ejb.parser.spec.AbstractMetaDataParser;
import org.jboss.metadata.javaee.spec.SecurityRoleMetaData;
import org.jboss.metadata.parser.ee.Element;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Parser for security-role elements
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class SecurityRoleMetaDataParser extends AbstractMetaDataParser<SecurityRoleMetaData> {

    public static final String LEGACY_NAMESPACE_URI = "urn:security-role";
    public static final String NAMESPACE_URI_1_0 = "urn:security-role:1.0";
    public static final String NAMESPACE_URI_2_0 = "urn:security-role:2.0";
    public static final SecurityRoleMetaDataParser INSTANCE = new SecurityRoleMetaDataParser();

    private SecurityRoleMetaDataParser() {

    }

    @Override
    public SecurityRoleMetaData parse(XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        SecurityRoleMetaData metaData = new SecurityRoleMetaData();
        processElements(metaData, reader, propertyReplacer);
        return metaData;
    }

    @Override
    protected void processElement(SecurityRoleMetaData metaData, XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        if (LEGACY_NAMESPACE_URI.equals(reader.getNamespaceURI()) ||
                NAMESPACE_URI_2_0.equals(reader.getNamespaceURI()) ||
                NAMESPACE_URI_1_0.equals(reader.getNamespaceURI())) {
            final String localName = reader.getLocalName();
            if (localName.equals(Element.ROLE_NAME.getLocalName())) {
                metaData.setRoleName(getElementText(reader, propertyReplacer));
            } else if (localName.equals(Element.PRINCIPAL_NAME.getLocalName())) {
                Set<String> principalNames = metaData.getPrincipals();
                if (principalNames == null) {
                    principalNames = new HashSet<String>();
                    metaData.setPrincipals(principalNames);
                }
                principalNames.add(getElementText(reader, propertyReplacer));
            }
            else
                throw unexpectedElement(reader);
        } else {
            super.processElement(metaData, reader, propertyReplacer);
        }
    }

}

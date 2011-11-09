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

package org.jboss.as.ejb3.security.parser;

import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.metadata.ejb.parser.spec.AbstractMetaDataParser;
import org.jboss.metadata.javaee.spec.SecurityRoleMetaData;
import org.jboss.metadata.parser.ee.Element;

/**
 * Parser for security-role elements
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class SecurityRoleMetaDataParser extends AbstractMetaDataParser<SecurityRoleMetaData> {

    @Override
    public SecurityRoleMetaData parse(XMLStreamReader reader) throws XMLStreamException {
        SecurityRoleMetaData metaData = new SecurityRoleMetaData();
        processElements(metaData, reader);
        return metaData;
    }

    @Override
    protected void processElement(SecurityRoleMetaData metaData, XMLStreamReader reader) throws XMLStreamException {
        if (reader.getNamespaceURI().equals("urn:security-role")) {
            final String localName = reader.getLocalName();
            if (localName.equals(Element.ROLE_NAME.getLocalName()))
                metaData.setRoleName(getElementText(reader));
            else if (localName.equals(Element.PRINCIPAL_NAME.getLocalName())) {
                Set<String> principalNames = metaData.getPrincipals();
                if (principalNames == null) {
                    principalNames = new HashSet<String>();
                    metaData.setPrincipals(principalNames);
                }
                principalNames.add(getElementText(reader));
            }
            else
                throw unexpectedElement(reader);
        } else
            super.processElement(metaData, reader);
    }

}

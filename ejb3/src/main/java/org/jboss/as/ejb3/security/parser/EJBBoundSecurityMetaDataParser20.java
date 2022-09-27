/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

import org.jboss.as.ejb3.security.metadata.EJBBoundSecurityMetaData;
import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaDataParser;
import org.jboss.metadata.property.PropertyReplacer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Parses 2.0 version of urn:security namespace.
 *
 * @author Emmanuel Hugonnet (c) 2022 Red Hat, Inc.
 */
public class EJBBoundSecurityMetaDataParser20 extends AbstractEJBBoundMetaDataParser<EJBBoundSecurityMetaData> {

    public static final EJBBoundSecurityMetaDataParser20 INSTANCE = new EJBBoundSecurityMetaDataParser20();
    public static final String NAMESPACE_URI_2_0 = "urn:security:2.0";

    protected EJBBoundSecurityMetaDataParser20() {

    }

    @Override
    public EJBBoundSecurityMetaData parse(XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        EJBBoundSecurityMetaData metaData = new EJBBoundSecurityMetaData();
        processElements(metaData, reader, propertyReplacer);
        return metaData;
    }

    @Override
    protected void processElement(EJBBoundSecurityMetaData metaData, XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        if (reader.getNamespaceURI().equals(NAMESPACE_URI_2_0)) {
            final String localName = reader.getLocalName();
            switch (localName) {
                case "security-domain":
                    metaData.setSecurityDomain(getElementText(reader, propertyReplacer));
                    break;
                case "run-as-principal":
                    metaData.setRunAsPrincipal(getElementText(reader, propertyReplacer));
                    break;
                case "missing-method-permissions-deny-access":
                    final String val = getElementText(reader, propertyReplacer);
                    metaData.setMissingMethodPermissionsDenyAccess(Boolean.valueOf(val.trim()));
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        } else {
            super.processElement(metaData, reader, propertyReplacer);
        }
    }


}

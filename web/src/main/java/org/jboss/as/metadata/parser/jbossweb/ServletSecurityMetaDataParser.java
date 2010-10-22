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

package org.jboss.as.metadata.parser.jbossweb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.web.spec.EmptyRoleSemanticType;
import org.jboss.metadata.web.spec.HttpMethodConstraintMetaData;
import org.jboss.metadata.web.spec.ServletSecurityMetaData;
import org.jboss.metadata.web.spec.TransportGuaranteeType;

/**
 * @author Remy Maucherat
 */
public class ServletSecurityMetaDataParser extends MetaDataElementParser {

    public static ServletSecurityMetaData parse(XMLStreamReader reader) throws XMLStreamException {
        ServletSecurityMetaData servletSecurity = new ServletSecurityMetaData();

        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case HTTP_METHOD_CONSTRAINT:
                    List<HttpMethodConstraintMetaData> httpMethodConstraints = servletSecurity.getHttpMethodConstraints();
                    if (httpMethodConstraints == null) {
                        httpMethodConstraints = new ArrayList<HttpMethodConstraintMetaData>();
                        servletSecurity.setHttpMethodConstraints(httpMethodConstraints);
                    }
                    httpMethodConstraints.add(HttpMethodConstraintMetaDataParser.parse(reader));
                    break;
                case EMPTY_ROLE_SEMANTIC:
                    servletSecurity.setEmptyRoleSemantic(EmptyRoleSemanticType.valueOf(reader.getElementText()));
                    break;
                case TRANSPORT_GUARANTEE:
                    servletSecurity.setTransportGuarantee(TransportGuaranteeType.valueOf(reader.getElementText()));
                    break;
                case ROLE_ALLOWED:
                    List<String> rolesAllowed = servletSecurity.getRolesAllowed();
                    if (rolesAllowed == null) {
                        rolesAllowed = new ArrayList<String>();
                        servletSecurity.setRolesAllowed(rolesAllowed);
                    }
                    rolesAllowed.add(reader.getElementText());
                    break;
                default: throw unexpectedElement(reader);
            }
        }

        return servletSecurity;
    }

}

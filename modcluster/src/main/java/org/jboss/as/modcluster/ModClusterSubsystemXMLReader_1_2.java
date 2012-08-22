/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.modcluster;

import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;

import java.util.List;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

public class ModClusterSubsystemXMLReader_1_2 extends ModClusterSubsystemXMLReader_1_1 implements XMLElementReader<List<ModelNode>> {

    void parseSSL(XMLExtendedStreamReader reader, List<ModelNode> list, PathAddress parent) throws XMLStreamException {
        PathAddress address = parent.append(ModClusterExtension.SSL_CONFIGURATION_PATH);
        final ModelNode ssl = Util.createAddOperation(address);
        list.add(ssl);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case KEY_ALIAS:
                case CERTIFICATE_KEY_FILE:
                case CIPHER_SUITE:
                case PROTOCOL:
                case CA_CERTIFICATE_FILE:
                case CA_REVOCATION_URL:
                case TRUSTSTORE_PASSWORD:
                case KEYSTORE_PASSWORD:
                    ModClusterSSLResourceDefinition.ATTRIBUTES_BY_NAME.get(attribute.getLocalName()).parseAndSetParameter(value, ssl, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        ParseUtils.requireNoContent(reader);
    }


}

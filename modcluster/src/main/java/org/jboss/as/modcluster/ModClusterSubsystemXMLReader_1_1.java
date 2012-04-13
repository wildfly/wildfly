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

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamException;
import java.util.List;

import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;

public class ModClusterSubsystemXMLReader_1_1 extends ModClusterSubsystemXMLReader_1_0 implements XMLElementReader<List<ModelNode>> {

    void parsePropConf(XMLExtendedStreamReader reader, ModelNode conf) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ADVERTISE_SOCKET:
                case PROXY_LIST:
                case PROXY_URL:
                case ADVERTISE:
                case ADVERTISE_SECURITY_KEY:
                case EXCLUDED_CONTEXTS:
                case AUTO_ENABLE_CONTEXTS:
                case STOP_CONTEXT_TIMEOUT:
                case SOCKET_TIMEOUT:
                case STICKY_SESSION:
                case STICKY_SESSION_REMOVE:
                case STICKY_SESSION_FORCE:
                case WORKER_TIMEOUT:
                case MAX_ATTEMPTS:
                case FLUSH_PACKETS:
                case FLUSH_WAIT:
                case PING:
                case SMAX:
                case TTL:
                case NODE_TIMEOUT:
                case BALANCER:
                case LOAD_BALANCING_GROUP:
                case CONNECTOR:
                    ModClusterConfigResourceDefinition.ATTRIBUTES_BY_NAME.get(attribute.getLocalName()).parseAndSetParameter(value, conf, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
    }


}

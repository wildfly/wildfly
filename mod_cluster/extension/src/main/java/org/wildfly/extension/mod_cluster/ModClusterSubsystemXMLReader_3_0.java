/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.extension.mod_cluster;

import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author Radoslav Husar
 */
public class ModClusterSubsystemXMLReader_3_0 extends ModClusterSubsystemXMLReader_1_0 implements XMLElementReader<List<ModelNode>> {

    void parsePropConf(XMLExtendedStreamReader reader, ModelNode conf) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ADVERTISE_SOCKET:
                case PROXY_URL:
                case ADVERTISE:
                case ADVERTISE_SECURITY_KEY:
                case EXCLUDED_CONTEXTS:
                case AUTO_ENABLE_CONTEXTS:
                case STOP_CONTEXT_TIMEOUT:
                case SOCKET_TIMEOUT:
                case SSL_CONTEXT:
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
                case STATUS_INTERVAL:
                case SESSION_DRAINING_STRATEGY:
                    ((SimpleAttributeDefinition) ModClusterConfigResourceDefinition.ATTRIBUTES_BY_NAME.get(attribute.getLocalName())).parseAndSetParameter(value, conf, reader);
                    break;
                case PROXIES:
                    ModClusterConfigResourceDefinition.PROXIES.getParser().parseAndSetParameter(ModClusterConfigResourceDefinition.PROXIES, value, conf, reader);
                    break;
                case PROXY_LIST:
                    // Keep deprecated PROXY_LIST to be able to support EAP 6.x slaves
                    ModClusterConfigResourceDefinition.PROXY_LIST.parseAndSetParameter(value, conf, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
    }


}

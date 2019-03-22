/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.session;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.web.session.SharedSessionManagerConfig;
import org.jboss.metadata.parser.jbossweb.ReplicationConfigParser;
import org.jboss.metadata.parser.servlet.SessionConfigMetaDataParser;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author Paul Ferraro
 */
public class SharedSessionConfigXMLReader implements XMLElementReader<SharedSessionManagerConfig> {

    private final SharedSessionConfigSchema schema;
    private final PropertyReplacer replacer;

    public SharedSessionConfigXMLReader(SharedSessionConfigSchema schema, PropertyReplacer replacer) {
        this.schema = schema;
        this.replacer = replacer;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void readElement(XMLExtendedStreamReader reader, SharedSessionManagerConfig result) throws XMLStreamException {

        ParseUtils.requireNoAttributes(reader);

        if (!this.schema.since(SharedSessionConfigSchema.VERSION_2_0)) {
            // Prior to 2.0, shared session manager was distributable by default
            result.setDistributable(true);
        }

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (reader.getLocalName()) {
                case "max-active-sessions": {
                    int value = Integer.parseInt(this.replacer.replaceProperties(reader.getElementText()));
                    if (value > 0) {
                        result.setMaxActiveSessions(value);
                    }
                    break;
                }
                case "replication-config": {
                    if (this.schema.since(SharedSessionConfigSchema.VERSION_2_0)) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    result.setReplicationConfig(ReplicationConfigParser.parse(reader, this.replacer));
                    break;
                }
                case "session-config": {
                    result.setSessionConfig(SessionConfigMetaDataParser.parse(reader, this.replacer));
                    break;
                }
                case "distributable": {
                    if (this.schema.since(SharedSessionConfigSchema.VERSION_2_0)) {
                        ParseUtils.requireNoAttributes(reader);
                        ParseUtils.requireNoContent(reader);
                        result.setDistributable(true);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }
}

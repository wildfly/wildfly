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

package org.jboss.as.messaging;

import javax.xml.stream.XMLStreamException;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;


/**
 * Messaging subsystem 1.4 XML parser.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a>
 *
 */
public class Messaging14SubsystemParser extends Messaging13SubsystemParser {

    protected Messaging14SubsystemParser() {
    }

    @Override
    protected void handleUnknownConfigurationAttribute(XMLExtendedStreamReader reader, Element element, ModelNode operation) throws XMLStreamException {
        switch (element) {
            case MAX_SAVED_REPLICATED_JOURNAL_SIZE:
                handleElementText(reader, element, operation);
                break;
            case OVERRIDE_IN_VM_SECURITY:
                // this element was added to 1.4 XSD in EAP by error but is
                // not actually handled by WildFly before its 3.0 schema
                handleElementText(reader, element, operation);
                break;
            default: {
                super.handleUnknownConfigurationAttribute(reader, element, operation);
            }
        }
    }

    @Override
    protected void handleUnknownGroupingHandlerAttribute(XMLExtendedStreamReader reader, Element element, ModelNode operation) throws XMLStreamException {
        switch(element) {
            case GROUP_TIMEOUT:
            case REAPER_PERIOD:
                handleElementText(reader, element, operation);
                break;
            default:
                super.handleUnknownGroupingHandlerAttribute(reader, element, operation);
        }
    }

    @Override
    protected void handleUnknownAddressSetting(XMLExtendedStreamReader reader, Element element, ModelNode addressSettingsAdd) throws XMLStreamException {
        switch (element) {
            case SLOW_CONSUMER_CHECK_PERIOD:
            case SLOW_CONSUMER_POLICY:
            case SLOW_CONSUMER_THRESHOLD:
                // these elements were added to 1.4 XSD in EAP by error but are
                // not actually handled by WildFly before its 3.0 schema
                handleElementText(reader, element, addressSettingsAdd);
                break;
            default:
                super.handleUnknownAddressSetting(reader, element, addressSettingsAdd);
        }
    }
}

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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.IN_VM_CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.REMOTE_CONNECTOR;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Messaging subsystem 2.0 XML parser.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a>
 *
 */
public class Messaging20SubsystemParser extends Messaging13SubsystemParser {

    private static final Messaging20SubsystemParser INSTANCE = new Messaging20SubsystemParser();

    public static MessagingSubsystemParser getInstance() {
        return INSTANCE;
    }

    private Messaging20SubsystemParser() {
    }

    @Override
    protected void handleUnknownConfigurationAttribute(XMLExtendedStreamReader reader, Element element, ModelNode operation) throws XMLStreamException {
        switch (element) {
            case MAX_SAVED_REPLICATED_JOURNAL_SIZE:
                handleElementText(reader, element, operation);
                break;
            default: {
                super.handleUnknownConfigurationAttribute(reader, element, operation);
            }
        }
    }

    @Override
    protected void handleUnknownBridgeAttribute(XMLExtendedStreamReader reader, Element element, ModelNode bridgeAdd) throws XMLStreamException {
        switch (element) {
            case RECONNECT_ATTEMPTS_ON_SAME_NODE:
                handleElementText(reader, element, bridgeAdd);
                break;
            default:
                super.handleUnknownBridgeAttribute(reader, element, bridgeAdd);
        }
    }

    @Override
    protected void handleUnknownAddressSetting(XMLExtendedStreamReader reader, Element element, ModelNode addressSettingsAdd) throws XMLStreamException {
        switch (element) {
            case EXPIRY_DELAY:
                handleElementText(reader, element, addressSettingsAdd);
                break;
            default:
                super.handleUnknownAddressSetting(reader, element, addressSettingsAdd);
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
}

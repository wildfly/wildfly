/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.dmr;

import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
class WSSubSystem20Reader extends WSSubSystem12Reader {
    WSSubSystem20Reader() {
    }

    @Override
    protected void readAttributes(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STATISTICS_ENABLED: {
                    Attributes.STATISTICS_ENABLED.parseAndSetParameter(value, operation, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
    }

    @Override
    protected void handleUnknownElement(final XMLExtendedStreamReader reader, final PathAddress parentAddress, final Element element, List<ModelNode> list, EnumSet<Element> encountered) throws XMLStreamException {
        //get the root ws subsystem add operation
        ModelNode operation = list.get(0);
        switch (element) {
            case WSDL_URI_SCHEME: {
                if (!encountered.add(element)) {
                    throw unexpectedElement(reader);
                }
                final String value = parseElementNoAttributes(reader);
                Attributes.WSDL_URI_SCHEME.parseAndSetParameter(value, operation, reader);
                break;
            }
            case WSDL_PATH_REWRITE_RULE: {
                final String value = parseElementNoAttributes(reader);
                Attributes.WSDL_PATH_REWRITE_RULE.parseAndSetParameter(value, operation, reader);
                break;
            }
            default: {
                super.handleUnknownElement(reader, parentAddress, element, list, encountered);
            }
        }
    }
}

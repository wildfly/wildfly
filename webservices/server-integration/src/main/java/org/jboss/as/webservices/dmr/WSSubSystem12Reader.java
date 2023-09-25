/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.dmr;

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
class WSSubSystem12Reader extends WSSubsystem11Reader {
    WSSubSystem12Reader() {
    }

    @Override
    protected void handleUnknownElement(final XMLExtendedStreamReader reader, final PathAddress parentAddress, final Element element, List<ModelNode> list, EnumSet<Element> encountered) throws XMLStreamException {
        switch (element) {
            case CLIENT_CONFIG: {
                List<ModelNode> configs = readConfig(reader, parentAddress, true);
                list.addAll(configs);
                break;
            }
            default: {
                super.handleUnknownElement(reader, parentAddress, element, list, encountered);
            }
        }
    }
}

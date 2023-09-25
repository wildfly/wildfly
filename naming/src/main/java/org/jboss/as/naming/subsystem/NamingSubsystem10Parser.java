/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming.subsystem;

import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author Stuart Douglas
 */
class NamingSubsystem10Parser implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

    private final boolean appclient;

    NamingSubsystem10Parser(boolean appclient) {
        this.appclient = appclient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        ParseUtils.requireNoAttributes(reader);
        ParseUtils.requireNoContent(reader);

        list.add(Util.createAddOperation(PathAddress.pathAddress(NamingExtension.SUBSYSTEM_PATH)));
        if(!appclient) {
            //we do not add remote naming to the application client
            //note that this is a bi
            list.add(Util.createAddOperation(PathAddress.pathAddress(NamingExtension.SUBSYSTEM_PATH).append(NamingSubsystemModel.SERVICE, NamingSubsystemModel.REMOTE_NAMING)));
        }
    }
}

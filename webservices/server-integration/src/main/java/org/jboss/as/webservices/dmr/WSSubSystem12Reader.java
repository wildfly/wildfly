/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.webservices.dmr;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
class WSSubSystem12Reader extends WSSubsystem11Reader {
    private static final WSSubSystem12Reader INSTANCE = new WSSubSystem12Reader();

    protected WSSubSystem12Reader() {
    }

    static WSSubSystem12Reader getInstance() {
        return INSTANCE;
    }

    @Override
    protected void handleUnknownElement(final XMLExtendedStreamReader reader, final PathAddress parentAddress, final Element element, List<ModelNode> list) throws XMLStreamException {
        switch (element) {
            case CLIENT_CONFIG: {
                List<ModelNode> configs = readConfig(reader, parentAddress, true);
                list.addAll(configs);
                break;
            }
            default: {
                super.handleUnknownElement(reader, parentAddress, element, list);
            }
        }
    }
}

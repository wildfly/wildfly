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

import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;

import javax.xml.stream.XMLStreamException;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
class WSSubSystem13Reader extends WSSubSystem12Reader {
    private static final WSSubSystem13Reader INSTANCE = new WSSubSystem13Reader();

    protected WSSubSystem13Reader() {
    }

    static WSSubSystem13Reader getInstance() {
        return INSTANCE;
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

}

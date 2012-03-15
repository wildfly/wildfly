/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.subsystem;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class EJB3Subsystem13Parser extends EJB3Subsystem12Parser {
    public static final EJB3Subsystem13Parser INSTANCE = new EJB3Subsystem13Parser();

    protected EJB3Subsystem13Parser() {
        super();
    }

    @Override
    protected EJB3SubsystemNamespace getExpectedNamespace() {
        return EJB3SubsystemNamespace.EJB3_1_3;
    }

    @Override
    protected void readAttribute(final ModelNode subsystemAddOperation, final XMLExtendedStreamReader reader, final int i) throws XMLStreamException {
        final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
        final String value = reader.getAttributeValue(i);
        switch (attribute) {
            case ENABLE_STATISTICS: {
                EJB3SubsystemRootResourceDefinition.ENABLE_STATISTICS.parseAndSetParameter(value, subsystemAddOperation, reader);
                break;
            }
            default: {
                super.readAttribute(subsystemAddOperation, reader, i);
            }
        }
    }

    @Override
    public void writeAttributes(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        final ModelNode model = context.getModelNode();

        EJB3SubsystemRootResourceDefinition.ENABLE_STATISTICS.marshallAsAttribute(model, writer);

        super.writeAttributes(writer, context);
    }
}

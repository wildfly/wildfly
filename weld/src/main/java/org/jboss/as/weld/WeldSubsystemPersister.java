/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld;

import static org.jboss.as.weld.WeldResourceDefinition.NON_PORTABLE_MODE_ATTRIBUTE;
import static org.jboss.as.weld.WeldResourceDefinition.REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Persists Weld subsystem configuration in the configuration xml file.
 *
 * @author Jozef Hartinger
 *
 */
class WeldSubsystemPersister implements XMLElementWriter<SubsystemMarshallingContext> {

    static final WeldSubsystemPersister INSTANCE = new WeldSubsystemPersister();

    private WeldSubsystemPersister() {
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter, final SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(WeldSubsystem20Parser.NAMESPACE, false);
        REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE.marshallAsAttribute(context.getModelNode(), streamWriter);
        NON_PORTABLE_MODE_ATTRIBUTE.marshallAsAttribute(context.getModelNode(), streamWriter);
        streamWriter.writeEndElement();
    }
}

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

package org.jboss.as.controller.resource;

import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Attribute for the list.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SocketBindingGroupIncludesAttribute extends PrimitiveListAttributeDefinition {

    public static final SocketBindingGroupIncludesAttribute INSTANCE = new SocketBindingGroupIncludesAttribute();

    private SocketBindingGroupIncludesAttribute() {
        super(ModelDescriptionConstants.INCLUDES, Element.INCLUDE.getLocalName(), true, ModelType.STRING, 0, Integer.MAX_VALUE,
                null, null, new StringLengthValidator(1, true), AttributeAccess.Flag.RESTART_JVM);
    }

    @Override
    public void marshallAsElement(ModelNode resourceModel, XMLStreamWriter writer) throws XMLStreamException {
        if (isMarshallable(resourceModel)) {
            for (ModelNode included : resourceModel.get(getName()).asList()) {
                writer.writeEmptyElement(getXmlName());
                writer.writeAttribute(Attribute.SOCKET_BINDING_GROUP.getLocalName(), included.asString());
            }
        }
    }

}

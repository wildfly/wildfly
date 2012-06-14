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

import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.List;

/**
 * {@link org.jboss.as.controller.AttributeDefinition} for the HQ server resource's remoting-interceptors attribute.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class RemotingInterceptorsAttribute extends PrimitiveListAttributeDefinition {

    public static final RemotingInterceptorsAttribute INSTANCE = new RemotingInterceptorsAttribute();

    private RemotingInterceptorsAttribute() {
        super(CommonAttributes.REMOTING_INTERCEPTORS_STRING, CommonAttributes.REMOTING_INTERCEPTORS_STRING, true, ModelType.STRING, 1,
                Integer.MAX_VALUE, null, null, new StringLengthValidator(1), AttributeAccess.Flag.RESTART_ALL_SERVICES);
    }

    @Override
    public void marshallAsElement(ModelNode resourceModel, XMLStreamWriter writer) throws XMLStreamException {
        if (resourceModel.hasDefined(getName())) {
            List<ModelNode> list = resourceModel.get(getName()).asList();
            if (list.size() > 0) {
                writer.writeStartElement(getXmlName());

                for (ModelNode child : list) {
                    writer.writeStartElement(Element.CLASS_NAME.getLocalName());
                    writer.writeCharacters(child.asString());
                    writer.writeEndElement();
                }

                writer.writeEndElement();
            }
        }
    }
}

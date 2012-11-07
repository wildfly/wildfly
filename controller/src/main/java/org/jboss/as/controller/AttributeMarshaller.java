/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public abstract class AttributeMarshaller {

    /**
     * Gets whether the given {@code resourceModel} has a value for this attribute that should be marshalled to XML.
     * <p>
     * This is the same as {@code isMarshallable(resourceModel, true)}.
     * </p>
     * @param attribute - attribute for which marshaling is being done
     * @param resourceModel the model, a non-null node of {@link org.jboss.dmr.ModelType#OBJECT}.
     * @return {@code true} if the given {@code resourceModel} has a defined value under this attribute's {@link AttributeDefinition#getName()} () name}.
     */
    public boolean isMarshallable(final AttributeDefinition attribute,final ModelNode resourceModel) {
        return isMarshallable(attribute,resourceModel, true);
    }

    /**
     * Gets whether the given {@code resourceModel} has a value for this attribute that should be marshalled to XML.
     *
     * @param attribute - attribute for which marshaling is being done
     * @param resourceModel   the model, a non-null node of {@link org.jboss.dmr.ModelType#OBJECT}.
     * @param marshallDefault {@code true} if the value should be marshalled even if it matches the default value
     * @return {@code true} if the given {@code resourceModel} has a defined value under this attribute's {@link AttributeDefinition#getName()} () name}
     *         and {@code marshallDefault} is {@code true} or that value differs from this attribute's {@link AttributeDefinition#getDefaultValue() default value}.
     */
    public boolean isMarshallable(final AttributeDefinition attribute, final ModelNode resourceModel, final boolean marshallDefault) {
        return resourceModel.hasDefined(attribute.getName()) && (marshallDefault || !resourceModel.get(attribute.getName()).equals(attribute.getDefaultValue()));
    }

    /**
     * Marshalls the value from the given {@code resourceModel} as an xml element, if it
     * {@link #isMarshallable(AttributeDefinition, org.jboss.dmr.ModelNode, boolean) is marshallable}.
     *
     * @param attribute - attribute for which marshaling is being done
     * @param resourceModel the model, a non-null node of {@link org.jboss.dmr.ModelType#OBJECT}.
     * @param writer        stream writer to use for writing the attribute
     * @throws javax.xml.stream.XMLStreamException
     *          if thrown by {@code writer}
     */

    public void marshallAsAttribute(final AttributeDefinition attribute,final ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException{
        throw ControllerMessages.MESSAGES.couldNotMarshalAttributeAsAttribute(attribute.getName());
    }

    public void marshallAsElement(final AttributeDefinition attribute, final ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException{
        throw ControllerMessages.MESSAGES.couldNotMarshalAttributeAsElement(attribute.getName());
    }
}

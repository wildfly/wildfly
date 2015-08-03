/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.MapAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * {@link AttributeParser} utilities.
 * @author Paul Ferraro
 */
public class AttributeParsers {

    public static final AttributeParser COLLECTION = new AttributeParser() {
        @Override
        public void parseAndSetParameter(AttributeDefinition attribute, String value, ModelNode operation, XMLStreamReader reader) throws XMLStreamException {
            if (attribute instanceof MapAttributeDefinition) {
                ((MapAttributeDefinition) attribute).parseAndAddParameterElement(value, reader.getElementText(), operation, (XMLExtendedStreamReader) reader);
            } else if (attribute instanceof ListAttributeDefinition) {
                ((ListAttributeDefinition) attribute).parseAndAddParameterElement(value, operation, reader);
            } else {
                super.parseAndSetParameter(attribute, value, operation, reader);
            }
        }
    };

    private AttributeParsers() {
        // Hide
    }
}

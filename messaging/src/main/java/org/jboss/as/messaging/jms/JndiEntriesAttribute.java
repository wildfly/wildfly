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

package org.jboss.as.messaging.jms;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.messaging.Attribute;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.Element;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.AttributeDefinition} for a JMS resource's {@code entries} attribute.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JndiEntriesAttribute extends ListAttributeDefinition {

    public static final JndiEntriesAttribute DESTINATION = new JndiEntriesAttribute(true);

    public static final JndiEntriesAttribute CONNECTION_FACTORY = new JndiEntriesAttribute(false);

    private static final String[] NO_BINDINGS = new String[0];

    private final boolean forDestination;

    private JndiEntriesAttribute(final boolean forDestination) {
        super(CommonAttributes.ENTRIES_STRING, CommonAttributes.ENTRIES_STRING, false, 1, Integer.MAX_VALUE, new StringLengthValidator(1));
        this.forDestination = forDestination;
    }

    @Override
    public void marshallAsElement(ModelNode resourceModel, XMLStreamWriter writer) throws XMLStreamException {
        if (resourceModel.hasDefined(getName())) {
            List<ModelNode> list = resourceModel.get(getName()).asList();
            if (list.size() > 0) {
                // This is a bit of a hack, using allowNull to distinguish the connection factory case
                // from the jms destination case
                if (!forDestination) {
                    writer.writeStartElement(getXmlName());
                }

                for (ModelNode child : list) {
                    writer.writeEmptyElement(Element.ENTRY.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), child.asString());
                }

                if (!forDestination) {
                    writer.writeEndElement();
                }
            }
        }
    }

    @Override
    protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
        setValueType(node);
    }

    @Override
    protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        setValueType(node);
    }

    @Override
    protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        setValueType(node);
    }

    private void setValueType(ModelNode node) {
        node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
    }

    public static String[] getJndiBindings(final ModelNode node) {
        if (node.isDefined()) {
            final Set<String> bindings = new HashSet<String>();
            for (final ModelNode entry : node.asList()) {
                bindings.add(entry.asString());
            }
            return bindings.toArray(new String[bindings.size()]);
        }
        return NO_BINDINGS;
    }
}

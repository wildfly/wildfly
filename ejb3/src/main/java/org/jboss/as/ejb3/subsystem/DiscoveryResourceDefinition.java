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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class DiscoveryResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition ABSTRACT_TYPE = new SimpleAttributeDefinitionBuilder(
            EJB3SubsystemModel.ABSTACT_TYPE, ModelType.STRING, true).setAllowExpression(true).build();

    public static final SimpleAttributeDefinition ABSTRACT_TYPE_AUTHORITY = new SimpleAttributeDefinitionBuilder(
            EJB3SubsystemModel.ABSTACT_TYPE_AUTHORITY, ModelType.STRING, true).setAllowExpression(true).build();

    public static final PropertiesAttributeDefinition URL_ATTRIBUTES = new PropertiesAttributeDefinition.Builder(EJB3SubsystemModel.ATTRIBUTES,true).build();

    public static final AttributeDefinition[] VALUE_TYPE_FIELDS = { ABSTRACT_TYPE, ABSTRACT_TYPE_AUTHORITY,  URL_ATTRIBUTES};

    public static final ObjectTypeAttributeDefinition STATIC_URL_TYPE =
            ObjectTypeAttributeDefinition.Builder.of(MODULE, VALUE_TYPE_FIELDS)
                    .setAttributeMarshaller(new AttributeMarshaller() {
                        @Override
                        public void marshallAsElement(final AttributeDefinition attributeDefinition, final ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
                            writer.writeStartElement("static-ejb-discovery");
                            for (Property attribute : resourceModel.get("attributes").asPropertyList()) {
                                writer.writeAttribute(attribute.getName(), attribute.getValue().asString());
                            }
                            ABSTRACT_TYPE.marshallAsAttribute(resourceModel, false, writer);
                            ABSTRACT_TYPE_AUTHORITY.marshallAsAttribute(resourceModel, false, writer);
                            writer.writeEndElement();
                        }
                    })
                    .build();

    public static final AttributeDefinition STATIC_URLS = ObjectListAttributeDefinition.Builder.of(EJB3SubsystemModel.STATIC_URLS, STATIC_URL_TYPE)
            .setAllowNull(true)
            .build();

    public static final Map<String, AttributeDefinition> ATTRIBUTES;

    static {
        Map<String, AttributeDefinition> map = new LinkedHashMap<String, AttributeDefinition>();
        map.put(STATIC_URLS.getName(), STATIC_URLS);

        ATTRIBUTES = Collections.unmodifiableMap(map);
    }

    public static final DiscoveryResourceDefinition INSTANCE = new DiscoveryResourceDefinition();

    private DiscoveryResourceDefinition(){
        super(PathElement.pathElement(EJB3SubsystemModel.DISCOVERY), EJB3Extension
                .getResourceDescriptionResolver(EJB3SubsystemModel.DISCOVERY), new RemotingProfileChildResourceAddHandler(
                ATTRIBUTES.values()), new RemotingProfileChildResourceRemoveHandler());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES.values()) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new RemotingProfileResourceChildWriteAttributeHandler(attr));
        }
    }
}

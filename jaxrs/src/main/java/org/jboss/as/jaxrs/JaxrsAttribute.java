/*
 * JBoss, Home of Professional Open Source
 * Copyright 2019, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.jaxrs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.DefaultAttributeMarshaller;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Jaxrs configuration attributes.
 *
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 */
public abstract class JaxrsAttribute {

    public static final String RESTEASY_PARAMETER_GROUP = "resteasy";

    public static final SimpleAttributeDefinition JAXRS_2_0_REQUEST_MATCHING = new SimpleAttributeDefinitionBuilder(JaxrsConstants.JAXRS_2_0_REQUEST_MATCHING, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.FALSE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    public static final SimpleAttributeDefinition RESTEASY_ADD_CHARSET = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_ADD_CHARSET, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.TRUE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    public static final SimpleAttributeDefinition RESTEASY_BUFFER_EXCEPTION_ENTITY = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_BUFFER_EXCEPTION_ENTITY, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.TRUE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    public static final SimpleAttributeDefinition RESTEASY_DISABLE_HTML_SANITIZER = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_DISABLE_HTML_SANITIZER, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.FALSE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    public static final StringListAttributeDefinition RESTEASY_DISABLE_PROVIDERS = new StringListAttributeDefinition.Builder(JaxrsConstants.RESTEASY_DISABLE_PROVIDERS)
            .setRequired(false)
            .setAllowExpression(true)
            .setAllowDuplicates(false)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .setAttributeMarshaller(ListMarshaller.INSTANCE)
            .build();

    public static final SimpleAttributeDefinition RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.FALSE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    public static final SimpleAttributeDefinition RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.TRUE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    public static final SimpleAttributeDefinition RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.TRUE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    public static final SimpleAttributeDefinition RESTEASY_GZIP_MAX_INPUT = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_GZIP_MAX_INPUT, ModelType.INT)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.INT, false))
            .setDefaultValue(new ModelNode(10000000))
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    public static final StringListAttributeDefinition RESTEASY_JNDI_RESOURCES = new StringListAttributeDefinition.Builder(JaxrsConstants.RESTEASY_JNDI_RESOURCES)
            .setRequired(false)
            .setAllowExpression(true)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .setAttributeMarshaller(ListMarshaller.INSTANCE)
            .build();

    public static final PropertiesAttributeDefinition RESTEASY_LANGUAGE_MAPPINGS = new PropertiesAttributeDefinition.Builder(JaxrsConstants.RESTEASY_LANGUAGE_MAPPINGS, true)
            .setRequired(false)
            .setAllowExpression(true)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .setAttributeMarshaller(MapMarshaller.INSTANCE)
            .build();

    public static final PropertiesAttributeDefinition RESTEASY_MEDIA_TYPE_MAPPINGS = new PropertiesAttributeDefinition.Builder(JaxrsConstants.RESTEASY_MEDIA_TYPE_MAPPINGS, true)
            .setRequired(false)
            .setAllowExpression(true)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .setAttributeMarshaller(MapMarshaller.INSTANCE)
            .build();

    public static final SimpleAttributeDefinition RESTEASY_MEDIA_TYPE_PARAM_MAPPING = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_MEDIA_TYPE_PARAM_MAPPING, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.STRING, true))
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    public static final SimpleAttributeDefinition RESTEASY_ORIGINAL_WEBAPPLICATIONEXCEPTION_BEHAVIOR = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_ORIGINAL_WEBAPPLICATIONEXCEPTION_BEHAVIOR, ModelType.BOOLEAN)
          .setRequired(false)
          .setAllowExpression(true)
          .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
          .setDefaultValue(ModelNode.FALSE)
          .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
          .build();

    public static final SimpleAttributeDefinition RESTEASY_PREFER_JACKSON_OVER_JSONB = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_PREFER_JACKSON_OVER_JSONB, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.FALSE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    public static final StringListAttributeDefinition RESTEASY_PROVIDERS = new StringListAttributeDefinition.Builder(JaxrsConstants.RESTEASY_PROVIDERS)
            .setRequired(false)
            .setAllowExpression(true)
            .setAllowDuplicates(false)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .setAttributeMarshaller(ListMarshaller.INSTANCE)
            .build();

    public static final SimpleAttributeDefinition RESTEASY_RFC7232_PRECONDITIONS = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_RFC7232_PRECONDITIONS, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.FALSE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    public static final SimpleAttributeDefinition RESTEASY_ROLE_BASED_SECURITY = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_ROLE_BASED_SECURITY, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.FALSE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    public static final SimpleAttributeDefinition RESTEASY_SECURE_RANDOM_MAX_USE = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_SECURE_RANDOM_MAX_USE, ModelType.INT)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.INT, false))
            .setDefaultValue(new ModelNode(100))
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    public static final SimpleAttributeDefinition RESTEASY_USE_BUILTIN_PROVIDERS = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_USE_BUILTIN_PROVIDERS, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.TRUE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    public static final SimpleAttributeDefinition RESTEASY_USE_CONTAINER_FORM_PARAMS = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_USE_CONTAINER_FORM_PARAMS, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.FALSE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    public static final SimpleAttributeDefinition RESTEASY_WIDER_REQUEST_MATCHING = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_WIDER_REQUEST_MATCHING, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.FALSE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] {
            JAXRS_2_0_REQUEST_MATCHING,
            RESTEASY_ADD_CHARSET,
            RESTEASY_BUFFER_EXCEPTION_ENTITY,
            RESTEASY_DISABLE_HTML_SANITIZER,
            RESTEASY_DISABLE_PROVIDERS,
            RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES,
            RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS,
            RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE,
            RESTEASY_GZIP_MAX_INPUT,
            RESTEASY_JNDI_RESOURCES,
            RESTEASY_LANGUAGE_MAPPINGS,
            RESTEASY_MEDIA_TYPE_MAPPINGS,
            RESTEASY_MEDIA_TYPE_PARAM_MAPPING,
            RESTEASY_ORIGINAL_WEBAPPLICATIONEXCEPTION_BEHAVIOR,
            RESTEASY_PREFER_JACKSON_OVER_JSONB,
            RESTEASY_PROVIDERS,
            RESTEASY_RFC7232_PRECONDITIONS,
            RESTEASY_ROLE_BASED_SECURITY,
            RESTEASY_SECURE_RANDOM_MAX_USE,
            RESTEASY_USE_BUILTIN_PROVIDERS,
            RESTEASY_USE_CONTAINER_FORM_PARAMS,
            RESTEASY_WIDER_REQUEST_MATCHING
    };

    public static final AttributeDefinition[] simpleAttributesArray = new AttributeDefinition[] {
            JAXRS_2_0_REQUEST_MATCHING,
            RESTEASY_ADD_CHARSET,
            RESTEASY_BUFFER_EXCEPTION_ENTITY,
            RESTEASY_DISABLE_HTML_SANITIZER,
            RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES,
            RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS,
            RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE,
            RESTEASY_GZIP_MAX_INPUT,
            RESTEASY_MEDIA_TYPE_PARAM_MAPPING,
            RESTEASY_ORIGINAL_WEBAPPLICATIONEXCEPTION_BEHAVIOR,
            RESTEASY_PREFER_JACKSON_OVER_JSONB,
            RESTEASY_RFC7232_PRECONDITIONS,
            RESTEASY_ROLE_BASED_SECURITY,
            RESTEASY_SECURE_RANDOM_MAX_USE,
            RESTEASY_USE_BUILTIN_PROVIDERS,
            RESTEASY_USE_CONTAINER_FORM_PARAMS,
            RESTEASY_WIDER_REQUEST_MATCHING
    };

    public static final AttributeDefinition[] listAttributeArray = new AttributeDefinition[] {
            RESTEASY_DISABLE_PROVIDERS,
            RESTEASY_PROVIDERS
    };

    public static final AttributeDefinition[] jndiAttributesArray = new AttributeDefinition[] {
            RESTEASY_JNDI_RESOURCES
    };

    public static AttributeDefinition[] mapAttributeArray = new AttributeDefinition[] {
            RESTEASY_LANGUAGE_MAPPINGS,
            RESTEASY_MEDIA_TYPE_MAPPINGS
    };

    public static final Set<AttributeDefinition> SIMPLE_ATTRIBUTES = new HashSet<>(Arrays. asList(simpleAttributesArray));
    public static final Set<AttributeDefinition> LIST_ATTRIBUTES = new HashSet<>(Arrays. asList(listAttributeArray));
    public static final Set<AttributeDefinition> JNDI_ATTRIBUTES = new HashSet<>(Arrays. asList(jndiAttributesArray));
    public static final Set<AttributeDefinition> MAP_ATTRIBUTES = new HashSet<>(Arrays. asList(mapAttributeArray));

    private static class ListMarshaller extends DefaultAttributeMarshaller {
        static final ListMarshaller INSTANCE = new ListMarshaller();

        @Override
        public void marshallAsElement(final AttributeDefinition attribute, final ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
            if (isMarshallable(attribute, resourceModel, marshallDefault)) {
                writer.writeStartElement(attribute.getXmlName());
                List<ModelNode> list = resourceModel.get(attribute.getName()).asList();
                for (ModelNode node : list) {
                    String child = "class";
                    if (JNDI_ATTRIBUTES.contains(attribute)) {
                        child = "jndi";
                    }
                    writer.writeStartElement(child);
                    writer.writeCharacters(node.asString().trim());
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
        }
    }

    private static class MapMarshaller extends DefaultAttributeMarshaller {
        static final MapMarshaller INSTANCE = new MapMarshaller();

        @Override
        public void marshallAsElement(final AttributeDefinition attribute, final ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
            if (isMarshallable(attribute, resourceModel, marshallDefault)) {
                writer.writeStartElement(attribute.getXmlName());
                List<ModelNode> list = resourceModel.get(attribute.getName()).asList();
                for (ModelNode node : list) {
                    Property property = node.asProperty();
                    writer.writeStartElement("entry");
                    writer.writeAttribute("key", property.getName());
                    writer.writeCharacters(property.getValue().asString().trim());
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
        }
    }
}

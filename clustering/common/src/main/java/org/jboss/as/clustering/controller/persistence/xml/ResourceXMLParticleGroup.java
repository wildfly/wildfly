/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import java.util.Map;

import org.jboss.as.clustering.controller.xml.QNameResolver;
import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.controller.xml.XMLContent;
import org.jboss.as.clustering.controller.xml.XMLParticleGroup;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.FeatureFilter;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * Encapsulates a group of XML particles for a subsystem resource, e.g. xs:choice, xs:sequence
 * @author Paul Ferraro
 */
public interface ResourceXMLParticleGroup extends ResourceXMLElementGroup, XMLParticleGroup<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> {

    interface Builder<T extends ResourceXMLParticleGroup, B extends Builder<T, B>> extends ResourceXMLElementGroup.Builder<T, B>, XMLParticleGroup.Builder<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode, T, B> {
    }

    abstract class AbstractBuilder<T extends ResourceXMLParticleGroup, B extends Builder<T, B>> extends XMLParticleGroup.AbstractBuilder<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode, T, B> implements Builder<T, B> {
        private final QNameResolver resolver;
        private volatile AttributeDefinitionXMLConfiguration configuration;

        AbstractBuilder(FeatureFilter filter, QNameResolver resolver, AttributeDefinitionXMLConfiguration configuration) {
            super(filter);
            this.resolver = resolver;
            this.configuration = configuration;
        }

        @Override
        public B addElements(Iterable<AttributeDefinition> attributes) {
            for (AttributeDefinition attribute : attributes) {
                if (this.enables(attribute)) {
                    AttributeParser parser = this.configuration.getParser(attribute);
                    AttributeMarshaller marshaller = this.configuration.getMarshaller(attribute);
                    if (parser.isParseAsElement() || marshaller.isMarshallableAsElement()) {
                        this.addElement(new AttributeDefinitionXMLElement.DefaultBuilder(attribute, this.resolver).withParser(parser).withMarshaller(marshaller).build());
                    }
                }
            }
            return this.builder();
        }

        @Override
        public B addElement(String groupName, Iterable<AttributeDefinition> attributes) {
            ResourceXMLElement.Builder builder = new ResourceXMLElement.DefaultBuilder(this.resolver.resolve(groupName), this, this.resolver, this.configuration);
            ResourceXMLSequence.Builder sequence = new ResourceXMLSequence.DefaultBuilder(this, this.resolver, this.configuration);
            boolean required = false;
            for (AttributeDefinition attribute : attributes) {
                if (this.enables(attribute)) {
                    required |= attribute.isRequired();
                    AttributeParser parser = this.configuration.getParser(attribute);
                    AttributeMarshaller marshaller = this.configuration.getMarshaller(attribute);
                    if (!parser.isParseAsElement() || !marshaller.isMarshallableAsElement()) {
                        builder.addAttribute(attribute);
                    }
                    if (parser.isParseAsElement() || marshaller.isMarshallableAsElement()) {
                        sequence.addElement(new AttributeDefinitionXMLElement.DefaultBuilder(attribute, this.resolver).withMarshaller(marshaller).withParser(parser).build());
                    }
                }
            }
            return this.addElement(builder.withCardinality(required ? XMLCardinality.Single.REQUIRED : XMLCardinality.Single.OPTIONAL).withContent(XMLContent.of(sequence.build())).build());
        }

        @Override
        public B withParsers(Map<AttributeDefinition, AttributeParser> parsers) {
            this.configuration = new AttributeDefinitionXMLConfiguration.DefaultAttributeDefinitionXMLConfiguration(this.configuration) {
                @Override
                public AttributeParser getParser(AttributeDefinition attribute) {
                    AttributeParser parser = parsers.get(attribute);
                    return (parser != null) ? parser : super.getParser(attribute);
                }
            };
            return this.builder();
        }

        @Override
        public B withMarshallers(Map<AttributeDefinition, AttributeMarshaller> marshallers) {
            this.configuration = new AttributeDefinitionXMLConfiguration.DefaultAttributeDefinitionXMLConfiguration(this.configuration) {
                @Override
                public AttributeMarshaller getMarshaller(AttributeDefinition attribute) {
                    AttributeMarshaller marshaller = marshallers.get(attribute);
                    return (marshaller != null) ? marshaller : super.getMarshaller(attribute);
                }
            };
            return this.builder();
        }
    }
}

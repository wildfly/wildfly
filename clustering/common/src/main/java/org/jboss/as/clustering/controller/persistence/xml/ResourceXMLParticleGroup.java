/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import java.util.Map;

import javax.xml.namespace.QName;

import org.jboss.as.clustering.controller.xml.QNameResolver;
import org.jboss.as.clustering.controller.xml.XMLParticleGroup;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.FeatureRegistry;
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

        AbstractBuilder(FeatureRegistry registry, QNameResolver resolver, AttributeDefinitionXMLConfiguration configuration) {
            super(registry);
            this.resolver = resolver;
            this.configuration = configuration;
        }

        @Override
        public B addElement(AttributeDefinition attribute) {
            if (this.enables(attribute)) {
                QName name = this.configuration.getName(attribute);
                AttributeParser parser = this.configuration.getParser(attribute);
                AttributeMarshaller marshaller = this.configuration.getMarshaller(attribute);
                if (parser.isParseAsElement() || marshaller.isMarshallableAsElement()) {
                    this.addElement(new AttributeDefinitionXMLElement.DefaultBuilder(attribute, this.resolver).withName(name).withParser(parser).withMarshaller(marshaller).build());
                }
            }
            return this.builder();
        }

        @Override
        public B addElements(Iterable<? extends AttributeDefinition> attributes) {
            for (AttributeDefinition attribute : attributes) {
                this.addElement(attribute);
            }
            return this.builder();
        }

        @Override
        public B withLocalNames(Map<AttributeDefinition, String> localNames) {
            this.configuration = new AttributeDefinitionXMLConfiguration.DefaultAttributeDefinitionXMLConfiguration(this.configuration) {
                @Override
                public QName getName(AttributeDefinition attribute) {
                    String localName = localNames.get(attribute);
                    return (localName != null) ? this.resolve(localName) : super.getName(attribute);
                }
            };
            return this.builder();
        }

        @Override
        public B withNames(Map<AttributeDefinition, QName> names) {
            this.configuration = new AttributeDefinitionXMLConfiguration.DefaultAttributeDefinitionXMLConfiguration(this.configuration) {
                @Override
                public QName getName(AttributeDefinition attribute) {
                    QName name = names.get(attribute);
                    return (name != null) ? name : super.getName(attribute);
                }
            };
            return this.builder();
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

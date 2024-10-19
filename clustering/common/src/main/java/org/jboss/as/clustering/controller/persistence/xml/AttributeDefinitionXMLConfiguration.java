/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import java.util.Map;

import javax.xml.namespace.QName;

import org.jboss.as.clustering.controller.xml.QNameResolver;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;

/**
 * Encapsulates XML overrides for a set of {@link AttributeDefinition}s.
 * Used to adapt the XML persistence behavior of an attribute to conform to an older version of a subsystem schema.
 * @author Paul Ferraro
 */
public interface AttributeDefinitionXMLConfiguration extends QNameResolver {

    interface Configurator<C extends Configurator<C>> {
        /**
         * Overrides the qualified names for the specified attributes.
         * @param names a mapping of qualified name overrides
         * @return a reference to this configurator
         */
        C withLocalNames(Map<AttributeDefinition, String> localNames);

        /**
         * Overrides the qualified names for the specified attributes.
         * @param names a mapping of qualified name overrides
         * @return a reference to this configurator
         */
        C withNames(Map<AttributeDefinition, QName> names);

        /**
         * Overrides the parsers for the specified attributes.
         * @param parsers a mapping of parser overrides
         * @return a reference to this configurator
         */
        C withParsers(Map<AttributeDefinition, AttributeParser> parsers);

        /**
         * Overrides the marshallers for the specified attributes.
         * @param marshallers a mapping of marshaller overrides
         * @return a reference to this configurator
         */
        C withMarshallers(Map<AttributeDefinition, AttributeMarshaller> marshallers);
    }

    /**
     * Returns the default {@link AttributeDefinition} XML configuration.
     * @param resolver a qualified name resolver
     * @return the default {@link AttributeDefinition} XML configuration.
     */
    static AttributeDefinitionXMLConfiguration of(QNameResolver resolver) {
        return new AttributeDefinitionXMLConfiguration() {
            @Override
            public QName resolve(String localName) {
                return resolver.resolve(localName);
            }
        };
    }

    /**
     * Returns the qualified name of the specified attribute.
     * @param attribute a resource attribute
     * @return the qualified name of the specified attribute.
     */
    default QName getName(AttributeDefinition attribute) {
        return this.resolve(this.getParser(attribute).getXmlName(attribute));
    }

    /**
     * Returns the parser of the specified attribute.
     * @param attribute a resource attribute
     * @return the parser of the specified attribute.
     */
    default AttributeParser getParser(AttributeDefinition attribute) {
        return attribute.getParser();
    }

    /**
     * Returns the marshaller of the specified attribute.
     * @param attribute a resource attribute
     * @return the marshaller of the specified attribute.
     */
    default AttributeMarshaller getMarshaller(AttributeDefinition attribute) {
        return attribute.getMarshaller();
    }

    class DefaultAttributeDefinitionXMLConfiguration implements AttributeDefinitionXMLConfiguration {
        private final AttributeDefinitionXMLConfiguration configuration;

        DefaultAttributeDefinitionXMLConfiguration(AttributeDefinitionXMLConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        public QName resolve(String localName) {
            return this.configuration.resolve(localName);
        }

        @Override
        public QName getName(AttributeDefinition attribute) {
            return this.configuration.getName(attribute);
        }

        @Override
        public AttributeParser getParser(AttributeDefinition attribute) {
            return this.configuration.getParser(attribute);
        }

        @Override
        public AttributeMarshaller getMarshaller(AttributeDefinition attribute) {
            return this.configuration.getMarshaller(attribute);
        }
    }
}

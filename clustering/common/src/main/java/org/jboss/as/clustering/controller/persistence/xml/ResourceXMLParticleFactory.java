/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import javax.xml.namespace.QName;

import org.jboss.as.clustering.controller.xml.QNameResolver;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.FeatureFilter;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.wildfly.common.Assert;

/**
 * A factory for building XML particles for a subsystem resource.
 * @author Paul Ferraro
 */
public interface ResourceXMLParticleFactory extends QNameResolver {

    static <S extends SubsystemSchema<S>> ResourceXMLParticleFactory newInstance(S schema) {
        return newInstance(schema, name -> new QName(schema.getNamespace().getUri(), name));
    }

    static ResourceXMLParticleFactory newInstance(FeatureFilter filter, QNameResolver resolver) {
        return new DefaultSubsystemXMLParticleFactory(filter, resolver);
    }

    /**
     * Returns a builder of an XML element for the subsystem resource with the specified registration.
     * @param registration a subsystem resource registration
     * @return a builder of an XML element for the subsystem resource with the specified registration.
     */
    SubsystemResourceRegistrationXMLElement.Builder subsystemElement(ResourceRegistration registration);

    /**
     * Returns a builder of an XML element for the singleton resource with the specified registration.
     * @param registration a subsystem resource registration
     * @return a builder of an XML element for the resource with the specified registration.
     */
    SingletonResourceRegistrationXMLElement.Builder singletonElement(ResourceRegistration registration);

    /**
     * Returns a builder of an XML element for the named resource with the specified registration.
     * @param registration a subsystem resource registration
     * @return a builder of an XML element for the resource with the specified registration.
     */
    NamedResourceRegistrationXMLElement.Builder namedElement(ResourceRegistration registration);

    /**
     * Returns a builder of an XML element for the specified attribute.
     * @param attribute a resource attribute
     * @return a builder of an XML element for the specified attribute.
     */
    AttributeDefinitionXMLElement.Builder element(AttributeDefinition attribute);

    /**
     * Returns a builder of an XML element using the specified name.
     * @param name a qualified element name
     * @return a builder of an XML element
     */
    ResourceXMLElement.Builder element(QName name);

    /**
     * Returns a builder of an XML choice for override resource registrations of the specified resource XML element.
     * @param element the XML element of a wildcard resource.
     * @return a builder of an XML choice for override resource registrations of the specified resource XML element.
     */
    ResourceRegistrationXMLChoice.Builder choice(NamedResourceRegistrationXMLElement element);

    /**
     * Returns a builder of an xs:all group.
     * @return a builder of an xs:all group
     */
    ResourceXMLAll.Builder all();

    /**
     * Returns a builder of an xs:sequence group.
     * @return a builder of an xs:sequence group
     */
    ResourceXMLSequence.Builder sequence();

    /**
     * Returns a builder of an xs:choice group.
     * @return a builder of an xs:choice group
     */
    ResourceXMLChoice.Builder choice();

    class DefaultSubsystemXMLParticleFactory implements ResourceXMLParticleFactory {
        private final FeatureFilter filter;
        private final QNameResolver resolver;

        DefaultSubsystemXMLParticleFactory(FeatureFilter filter, QNameResolver resolver) {
            this.filter = filter;
            this.resolver = resolver;
        }

        @Override
        public QName resolve(String localName) {
            return this.resolver.resolve(localName);
        }

        @Override
        public SubsystemResourceRegistrationXMLElement.Builder subsystemElement(ResourceRegistration registration) {
            Assert.assertTrue(registration.getPathElement().getKey().equals(ModelDescriptionConstants.SUBSYSTEM));
            return new SubsystemResourceRegistrationXMLElement.DefaultBuilder(registration, this.filter, this.resolver);
        }

        @Override
        public SingletonResourceRegistrationXMLElement.Builder singletonElement(ResourceRegistration registration) {
            Assert.assertFalse(registration.getPathElement().isWildcard());
            return new SingletonResourceRegistrationXMLElement.DefaultBuilder(registration, this.filter, this.resolver);
        }

        @Override
        public NamedResourceRegistrationXMLElement.Builder namedElement(ResourceRegistration registration) {
            return new NamedResourceRegistrationXMLElement.DefaultBuilder(registration, this.filter, this.resolver);
        }

        @Override
        public AttributeDefinitionXMLElement.Builder element(AttributeDefinition attribute) {
            return new AttributeDefinitionXMLElement.DefaultBuilder(attribute, this.resolver);
        }

        @Override
        public ResourceXMLElement.Builder element(QName name) {
            return new ResourceXMLElement.DefaultBuilder(name, this.filter, this.resolver);
        }

        @Override
        public ResourceRegistrationXMLChoice.Builder choice(NamedResourceRegistrationXMLElement element) {
            Assert.assertTrue(element.getPathElement().isWildcard());
            return new ResourceRegistrationXMLChoice.DefaultBuilder(element, this.filter);
        }

        @Override
        public ResourceXMLChoice.Builder choice() {
            return new ResourceXMLChoice.DefaultBuilder(this.filter, this.resolver);
        }

        @Override
        public ResourceXMLAll.Builder all() {
            return new ResourceXMLAll.DefaultBuilder(this.filter, this.resolver);
        }

        @Override
        public ResourceXMLSequence.Builder sequence() {
            return new ResourceXMLSequence.DefaultBuilder(this.filter, this.resolver);
        }
    }
}

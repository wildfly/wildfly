/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import javax.xml.namespace.QName;

import org.jboss.as.clustering.controller.xml.QNameResolver;
import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.version.Stability;
import org.wildfly.common.Assert;

/**
 * A factory for building XML particles for a subsystem resource.
 * N.B. Unless otherwise specified, all particles created by this factory will have a default cardinality of {@link org.jboss.as.controller.xml.XMLCardinality.Single#REQUIRED}.
 * @author Paul Ferraro
 */
public interface ResourceXMLParticleFactory extends FeatureRegistry, QNameResolver {

    /**
     * Creates a new particle factory for the specified subsystem schema.
     * @param <S> the subsystem schema type
     * @param schema a subsystem schema
     * @return a particle factory
     */
    static <S extends SubsystemSchema<S>> ResourceXMLParticleFactory newInstance(S schema) {
        return newInstance(schema, name -> new QName(schema.getNamespace().getUri(), name));
    }

    /**
     * Creates a new particle factory for the specified feature registry and qualified name resolver.
     * @param registry a feature registry
     * @param resolver a qualified name resolver
     * @return a particle factory
     */
    static ResourceXMLParticleFactory newInstance(FeatureRegistry registry, QNameResolver resolver) {
        return new DefaultSubsystemXMLParticleFactory(registry, resolver);
    }

    /**
     * Returns a builder of an XML element for the subsystem resource with the specified registration.
     * Default element local name is {@link ResourceXMLElementLocalName#KEY}.
     * @param registration a subsystem resource registration
     * @return a builder of an XML element for the subsystem resource with the specified registration.
     */
    SubsystemResourceRegistrationXMLElement.Builder subsystemElement(ResourceRegistration registration);

    /**
     * Returns a builder of an XML element for the named resource with the specified registration.
     * Default element local name is {@link ResourceXMLElementLocalName#KEY}.
     * Wildcard resource registrations will use unbounded cardinality, by default.
     * @param registration a subsystem resource registration
     * @return a builder of an XML element for the resource with the specified registration.
     */
    NamedResourceRegistrationXMLElement.Builder namedElement(ResourceRegistration registration);

    /**
     * Returns a builder of an XML choice of override resource registrations of the specified resource XML element.
     * By default, this choice will use the cardinality of the specified element.
     * @param element the XML element of a wildcard resource.
     * @return a builder of an XML choice for override resource registrations of the specified resource XML element.
     */
    NamedResourceRegistrationXMLChoice.Builder namedChoice(NamedResourceRegistrationXMLElement element);

    /**
     * Returns a builder of an XML element for the singleton resource with the specified registration.
     * Default element local name is {@link ResourceXMLElementLocalName#VALUE}.
     * @param registration a subsystem resource registration
     * @return a builder of an XML element for the resource with the specified registration.
     */
    SingletonResourceRegistrationXMLElement.Builder singletonElement(ResourceRegistration registration);

    /**
     * Returns a builder of an optional XML choice of singleton resource registration elements.
     * When no element choices are present, the specified resource registration is implied.
     * By default, this choice will use cardinality {@link org.jboss.as.controller.xml.XMLCardinality.Single#OPTIONAL} by default.
     * @param element the XML element of a wildcard resource.
     * @return a builder of an XML choice for override resource registrations of the specified resource XML element.
     */
    SingletonResourceRegistrationXMLChoice.Builder singletonChoice(ResourceRegistration implied);

    /**
     * Returns a builder of an XML element using the specified name.
     * @param name a qualified element name
     * @return a builder of an XML element
     */
    default ResourceXMLElement.Builder element(QName name) {
        return this.element(name, this.getStability());
    }

    /**
     * Returns a builder of an XML element using the specified name.
     * Default cardinality is {@link org.jboss.as.controller.xml.XMLCardinality.Single#REQUIRED}.
     * @param name a qualified element name
     * @param stability the stability of this element
     * @return a builder of an XML element
     */
    ResourceXMLElement.Builder element(QName name, Stability stability);

    /**
     * Returns a builder of an xs:all group.
     * Default cardinality is {@link org.jboss.as.controller.xml.XMLCardinality.Single#REQUIRED}.
     * @return a builder of an xs:all group
     */
    ResourceXMLAll.Builder all();

    /**
     * Returns a builder of an xs:sequence group.
     * Default cardinality is {@link org.jboss.as.controller.xml.XMLCardinality.Single#REQUIRED}.
     * @return a builder of an xs:sequence group
     */
    ResourceXMLSequence.Builder sequence();

    /**
     * Returns a builder of an xs:choice group.
     * Default cardinality is {@link org.jboss.as.controller.xml.XMLCardinality.Single#REQUIRED}.
     * @return a builder of an xs:choice group
     */
    ResourceXMLChoice.Builder choice();

    class DefaultSubsystemXMLParticleFactory implements ResourceXMLParticleFactory {
        private final FeatureRegistry registry;
        private final QNameResolver resolver;

        DefaultSubsystemXMLParticleFactory(FeatureRegistry registry, QNameResolver resolver) {
            this.registry = registry;
            this.resolver = resolver;
        }

        @Override
        public QName resolve(String localName) {
            return this.resolver.resolve(localName);
        }

        @Override
        public Stability getStability() {
            return this.registry.getStability();
        }

        @Override
        public SubsystemResourceRegistrationXMLElement.Builder subsystemElement(ResourceRegistration registration) {
            // Validate that PathElement of resource registration refers to a subsystem
            Assert.assertFalse(registration.getPathElement().isWildcard());
            Assert.assertTrue(registration.getPathElement().getKey().equals(ModelDescriptionConstants.SUBSYSTEM));
            return new SubsystemResourceRegistrationXMLElement.DefaultBuilder(registration, this.registry, this.resolver);
        }

        @Override
        public NamedResourceRegistrationXMLElement.Builder namedElement(ResourceRegistration registration) {
            // May be wildcard or override resource registration
            return new NamedResourceRegistrationXMLElement.DefaultBuilder(registration, this.registry, this.resolver);
        }

        @Override
        public NamedResourceRegistrationXMLChoice.Builder namedChoice(NamedResourceRegistrationXMLElement element) {
            Assert.assertTrue(element.getPathElement().isWildcard());
            return new NamedResourceRegistrationXMLChoice.DefaultBuilder(element, this.registry);
        }

        @Override
        public SingletonResourceRegistrationXMLElement.Builder singletonElement(ResourceRegistration registration) {
            // Only allow non-wildcard resource registrations
            Assert.assertFalse(registration.getPathElement().isWildcard());
            return new SingletonResourceRegistrationXMLElement.DefaultBuilder(registration, this.registry, this.resolver);
        }

        @Override
        public SingletonResourceRegistrationXMLChoice.Builder singletonChoice(ResourceRegistration implied) {
            // Only allow non-wildcard resource registrations
            Assert.assertFalse(implied.getPathElement().isWildcard());
            return new SingletonResourceRegistrationXMLChoice.DefaultBuilder(implied, this.registry);
        }

        @Override
        public ResourceXMLElement.Builder element(QName name, Stability stability) {
            return new ResourceXMLElement.DefaultBuilder(name, stability, this.registry, this.resolver);
        }

        @Override
        public ResourceXMLChoice.Builder choice() {
            return new ResourceXMLChoice.DefaultBuilder(this.registry, this.resolver);
        }

        @Override
        public ResourceXMLAll.Builder all() {
            return new ResourceXMLAll.DefaultBuilder(this.registry, this.resolver);
        }

        @Override
        public ResourceXMLSequence.Builder sequence() {
            return new ResourceXMLSequence.DefaultBuilder(this.registry, this.resolver);
        }
    }
}

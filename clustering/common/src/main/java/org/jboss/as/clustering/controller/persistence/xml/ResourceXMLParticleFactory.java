/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import javax.xml.namespace.QName;

import org.jboss.as.clustering.controller.xml.QNameResolver;
import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.version.Stability;

/**
 * A factory for building XML particles for a subsystem resource.
 * All particles created by this factory will have a default cardinality of {@link org.jboss.as.controller.xml.XMLCardinality.Single#REQUIRED}.
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
     * @param registration a subsystem resource registration
     * @return a builder of an XML element for the resource with the specified registration.
     */
    NamedResourceRegistrationXMLElement.Builder namedElement(ResourceRegistration registration);

    /**
     * Returns a builder of an XML choice of one or more {@link NamedResourceRegistrationXMLElement}, including the specified {@link NamedResourceRegistrationXMLElement}.
     * Elements added to this choice must be override registrations of the specified wildcard {@link NamedResourceRegistrationXMLElement}.
     * @param element the XML element for a wildcard resource registration.
     * @return a builder of an XML choice of one or more {@link NamedResourceRegistrationXMLElement}.
     * @throws IllegalArgumentException if the specified {@link NamedResourceRegistrationXMLElement} is not associated with a wildcard resource.
     */
    NamedResourceRegistrationXMLChoice.Builder namedElementChoice(NamedResourceRegistrationXMLElement element);

    /**
     * Returns a builder of an XML element for the singleton resource with the specified registration.
     * Default element local name is {@link ResourceXMLElementLocalName#VALUE}.
     * @param registration a subsystem resource registration
     * @return a builder of an XML element for the resource with the specified registration.
     */
    SingletonResourceRegistrationXMLElement.Builder singletonElement(ResourceRegistration registration);

    /**
     * Returns a builder of an XML choice of one or more {@link SingletonResourceRegistrationXMLElement}.
     * A {@link SingletonResourceRegistrationXMLChoice} may designate a {@link ResourceRegistration} to be assumed when no choice is present.
     * @return a builder of an XML choice of one or more {@link SingletonResourceRegistrationXMLElement}
     */
    SingletonResourceRegistrationXMLChoice.Builder singletonElementChoice();

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
            PathElement path = registration.getPathElement();
            if (path.isWildcard() || !path.getKey().equals(ModelDescriptionConstants.SUBSYSTEM)) {
                throw ClusteringLogger.ROOT_LOGGER.invalidSubsystemPath(path);
            }
            return new SubsystemResourceRegistrationXMLElement.DefaultBuilder(registration, this.registry, this.resolver);
        }

        @Override
        public NamedResourceRegistrationXMLElement.Builder namedElement(ResourceRegistration registration) {
            // Allow both wildcard or override resource registration
            return new NamedResourceRegistrationXMLElement.DefaultBuilder(registration, this.registry, this.resolver);
        }

        @Override
        public NamedResourceRegistrationXMLChoice.Builder namedElementChoice(NamedResourceRegistrationXMLElement element) {
            if (!element.getPathElement().isWildcard()) {
                throw ClusteringLogger.ROOT_LOGGER.nonWildcardPathNotAllowed(element.getPathElement());
            }
            return new NamedResourceRegistrationXMLChoice.DefaultBuilder(element, this.registry);
        }

        @Override
        public SingletonResourceRegistrationXMLElement.Builder singletonElement(ResourceRegistration registration) {
            if (registration.getPathElement().isWildcard()) {
                throw ClusteringLogger.ROOT_LOGGER.wildcardPathNotAllowed(registration.getPathElement());
            }
            return new SingletonResourceRegistrationXMLElement.DefaultBuilder(registration, this.registry, this.resolver);
        }

        @Override
        public SingletonResourceRegistrationXMLChoice.Builder singletonElementChoice() {
            return new SingletonResourceRegistrationXMLChoice.DefaultBuilder(this.registry);
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

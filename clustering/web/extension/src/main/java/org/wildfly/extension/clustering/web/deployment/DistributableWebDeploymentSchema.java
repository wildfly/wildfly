/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.deployment;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.xml.IntVersionSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.controller.xml.XMLAttribute;
import org.jboss.as.controller.xml.XMLCardinality;
import org.jboss.as.controller.xml.XMLChoice;
import org.jboss.as.controller.xml.XMLComponentFactory;
import org.jboss.as.controller.xml.XMLContent;
import org.jboss.as.controller.xml.XMLElement;
import org.jboss.as.controller.xml.XMLElementSchema;
import org.jboss.as.controller.xml.XMLSequence;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.jbossallxml.JBossAllSchema;
import org.jboss.staxmapper.IntVersion;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.extension.clustering.web.routing.LocalRouteLocatorProvider;
import org.wildfly.extension.clustering.web.routing.NullRouteLocatorProvider;
import org.wildfly.extension.clustering.web.routing.infinispan.PrimaryOwnerRouteLocatorProvider;
import org.wildfly.extension.clustering.web.routing.infinispan.RankedRouteLocatorProvider;
import org.wildfly.extension.clustering.web.session.DistributableSessionManagementProviderFactory;
import org.wildfly.extension.clustering.web.session.hotrod.HotRodSessionManagementProvider;
import org.wildfly.extension.clustering.web.session.infinispan.InfinispanSessionManagementProvider;

/**
 * Enumerate the schema versions of the distibutable-web deployment descriptor.
 * @author Paul Ferraro
 */
public enum DistributableWebDeploymentSchema implements XMLElementSchema<DistributableWebDeploymentSchema, MutableDistributableWebDeploymentConfiguration>, JBossAllSchema<DistributableWebDeploymentSchema, DistributableWebDeploymentConfiguration> {

    VERSION_1_0(1, 0),
    VERSION_2_0(2, 0),
    VERSION_3_0(3, 0),
    VERSION_4_0(4, 0),
    ;
    private final VersionedNamespace<IntVersion, DistributableWebDeploymentSchema> namespace;
    private final XMLComponentFactory<MutableDistributableWebDeploymentConfiguration, Void> factory = XMLComponentFactory.newInstance(this);

    DistributableWebDeploymentSchema(int major, int minor) {
        this.namespace = IntVersionSchema.createURN(List.of(IntVersionSchema.JBOSS_IDENTIFIER, this.getLocalName()), new IntVersion(major, minor));
    }

    @Override
    public String getLocalName() {
        return "distributable-web";
    }

    @Override
    public VersionedNamespace<IntVersion, DistributableWebDeploymentSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, MutableDistributableWebDeploymentConfiguration configuration) throws XMLStreamException {
        XMLElement<MutableDistributableWebDeploymentConfiguration, Void> element = this.factory.element(this.getQualifiedName())
                .withContent(this.sessionManagementChoice(configuration))
                .build();
        element.getReader().readElement(reader, configuration);
    }

    @Override
    public DistributableWebDeploymentConfiguration parse(XMLExtendedStreamReader reader, DeploymentUnit unit) throws XMLStreamException {
        MutableDistributableWebDeploymentConfiguration configuration = new MutableDistributableWebDeploymentConfiguration(unit);
        this.readElement(reader, configuration);
        return configuration;
    }

    private XMLChoice<MutableDistributableWebDeploymentConfiguration, Void> sessionManagementChoice(MutableDistributableWebDeploymentConfiguration config) {
        return this.factory.choice()
                .addElement(this.sessionManagementElement())
                .addElement(this.infinispanSessionManagementElement(config))
                .addElement(this.hotrodSessionManagementElement(config))
                .build();
    }

    private XMLElement<MutableDistributableWebDeploymentConfiguration, Void> sessionManagementElement() {
        return this.factory.element(this.resolve("session-management"))
                .addAttribute(this.factory.attribute(this.resolve("name")).withConsumer(MutableDistributableWebDeploymentConfiguration::setSessionManagementName).build())
                .withContent(this.factory.sequence().withCardinality(XMLCardinality.Single.OPTIONAL).addElement(this.immutableClassElement(this.factory)).build())
                .build();
    }

    private XMLElement<MutableDistributableWebDeploymentConfiguration, Void> infinispanSessionManagementElement(MutableDistributableWebDeploymentConfiguration config) {
        XMLComponentFactory<MutableInfinispanSessionManagementConfiguration, Void> factory = XMLComponentFactory.newInstance(this);
        List<XMLAttribute<MutableInfinispanSessionManagementConfiguration, Void>> attributes = List.of(
                factory.attribute(this.resolve("cache-container")).withUsage(XMLAttribute.Use.REQUIRED).withConsumer(MutableInfinispanSessionManagementConfiguration::setContainerName).build(),
                factory.attribute(this.resolve("cache")).withConsumer(MutableInfinispanSessionManagementConfiguration::setCacheName).build());
        XMLChoice.Builder<MutableInfinispanSessionManagementConfiguration, Void> affinityChoiceBuilder = this.affinityChoiceBuilder(factory)
                .addElement(this.affinityElement("primary-owner-affinity", PrimaryOwnerRouteLocatorProvider::new))
                ;
        if (this.since(VERSION_2_0)) {
            affinityChoiceBuilder.addElement(this.rankedAffinityElement());
        }
        XMLChoice<MutableInfinispanSessionManagementConfiguration, Void> affinityChoice = affinityChoiceBuilder.build();
        return this.sessionManagementElement(config, MutableInfinispanSessionManagementConfiguration::new, factory, "infinispan-session-management", attributes, affinityChoice, InfinispanSessionManagementProvider::new);
    }

    private XMLElement<MutableDistributableWebDeploymentConfiguration, Void> hotrodSessionManagementElement(MutableDistributableWebDeploymentConfiguration config) {
        XMLComponentFactory<MutableHotRodSessionManagementConfiguration, Void> factory = XMLComponentFactory.newInstance(this);
        List<XMLAttribute<MutableHotRodSessionManagementConfiguration, Void>> attributes = new LinkedList<>();
        attributes.add(factory.attribute(this.resolve("remote-cache-container")).withUsage(XMLAttribute.Use.REQUIRED).withConsumer(MutableHotRodSessionManagementConfiguration::setContainerName).build());
        attributes.add(factory.attribute(this.resolve("cache-configuration")).withConsumer(MutableHotRodSessionManagementConfiguration::setConfigurationName).build());
        if (this.since(VERSION_4_0)) {
            attributes.add(factory.attribute(this.resolve("expiration-thread-pool-size")).build());
        }
        XMLChoice<MutableHotRodSessionManagementConfiguration, Void> affinityChoice = this.affinityChoiceBuilder(factory).build();
        return this.sessionManagementElement(config, MutableHotRodSessionManagementConfiguration::new, factory, "hotrod-session-management", attributes, affinityChoice, HotRodSessionManagementProvider::new);
    }

    private <C extends MutableSessionManagementConfiguration> XMLElement<MutableDistributableWebDeploymentConfiguration, Void> sessionManagementElement(MutableDistributableWebDeploymentConfiguration config, BiFunction<UnaryOperator<String>, Consumer<String>, C> configFactory, XMLComponentFactory<C, Void> factory, String localName, Iterable<XMLAttribute<C, Void>> attributes, XMLChoice<C, Void> affinityChoice, DistributableSessionManagementProviderFactory providerFactory) {
        XMLElement.Builder<C, Void> builder = factory.element(this.resolve(localName))
                .addAttributes(attributes)
                .addAttribute(factory.attribute(this.resolve("granularity")).withUsage(XMLAttribute.Use.REQUIRED).withConsumer(MutableSessionManagementConfiguration::setSessionGranularity).build())
                ;
        if (this.since(VERSION_3_0)) {
            builder.addAttribute(factory.attribute(this.resolve("marshaller")).withConsumer(MutableSessionManagementConfiguration::setMarshallerFactory).build());
        }
        XMLSequence<C, Void> sequence = factory.sequence()
                .addChoice(affinityChoice)
                .addElement(this.immutableClassElement(factory))
                .build();
        XMLElement<C, Void> element = builder.withContent(sequence).build();
        return element.withContext(() -> configFactory.apply(config, config), new BiConsumer<>() {
            @Override
            public void accept(MutableDistributableWebDeploymentConfiguration config, C sessionManagementConfig) {
                config.setSessionManagementProvider(providerFactory.createSessionManagementProvider(sessionManagementConfig, sessionManagementConfig, sessionManagementConfig.getRouteLocatorProvider()));
            }
        }, Function.identity());
    }

    private <C extends Consumer<String>> XMLElement<C, Void> immutableClassElement(XMLComponentFactory<C, Void> factory) {
        return factory.element(this.resolve("immutable-class")).withContent(XMLContent.of(Consumer::accept)).withCardinality(XMLCardinality.Unbounded.OPTIONAL).build();
    }

    private <C extends MutableSessionManagementConfiguration> XMLChoice.Builder<C, Void> affinityChoiceBuilder(XMLComponentFactory<C, Void> factory) {
        return factory.choice()
                .addElement(this.affinityElement("no-affinity", NullRouteLocatorProvider::new))
                .addElement(this.affinityElement("local-affinity", LocalRouteLocatorProvider::new));
    }

    private <C extends MutableSessionManagementConfiguration> XMLElement<C, Void> affinityElement(String localName, Supplier<RouteLocatorProvider> contextFactory) {
        XMLComponentFactory<RouteLocatorProvider, Void> factory = XMLComponentFactory.newInstance(this);
        return factory.element(this.resolve(localName)).build().withContext(contextFactory, MutableSessionManagementConfiguration::setRouteLocatorProvider, Function.identity());
    }

    private <C extends MutableSessionManagementConfiguration> XMLElement<C, Void> rankedAffinityElement() {
        XMLComponentFactory<MutableRankedRoutingConfiguration, Void> factory = XMLComponentFactory.newInstance(this);
        XMLElement<MutableRankedRoutingConfiguration, Void> element = factory.element(this.resolve("ranked-affinity"))
                .addAttribute(factory.attribute(this.resolve("delimiter")).withConsumer(MutableRankedRoutingConfiguration::setDelimiter).build())
                .addAttribute(factory.attribute(this.resolve("max-routes")).withConsumer(MutableRankedRoutingConfiguration::setMaxMembers).build())
                .build();
        return element.withContext(MutableRankedRoutingConfiguration::new, new BiConsumer<>() {
            @Override
            public void accept(C config, MutableRankedRoutingConfiguration routingConfig) {
                config.setRouteLocatorProvider(new RankedRouteLocatorProvider(routingConfig));
            }
        }, Function.identity());
    }
}

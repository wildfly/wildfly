/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.EnumSet;
import java.util.List;

import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.persistence.xml.NamedResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.ResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.ResourceXMLChoice;
import org.jboss.as.controller.persistence.xml.ResourceXMLElementLocalName;
import org.jboss.as.controller.persistence.xml.ResourceXMLParticleFactory;
import org.jboss.as.controller.persistence.xml.ResourceXMLSequence;
import org.jboss.as.controller.persistence.xml.SingletonResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.SubsystemResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.SubsystemResourceXMLSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.controller.xml.XMLCardinality;
import org.jboss.staxmapper.IntVersion;
import org.wildfly.clustering.server.service.CacheConfigurationAttributeGroup;
import org.wildfly.subsystem.resource.ResourceDescriptor;

/**
 * Enumerates the schema versions for the distributable-web subsystem.
 * @author Paul Ferraro
 */
public enum DistributableWebSubsystemSchema implements SubsystemResourceXMLSchema<DistributableWebSubsystemSchema> {

    VERSION_1_0(1, 0), // WildFly 17
    VERSION_2_0(2, 0), // WildFly 18-26.1, EAP 7.4
    VERSION_3_0(3, 0), // WildFly 27-29
    VERSION_4_0(4, 0), // WildFly 30-present, EAP 8.0
    ;
    static final DistributableWebSubsystemSchema CURRENT = VERSION_4_0;

    private final ResourceXMLParticleFactory factory = ResourceXMLParticleFactory.newInstance(this);
    private final VersionedNamespace<IntVersion, DistributableWebSubsystemSchema> namespace;

    DistributableWebSubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(DistributableWebSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, DistributableWebSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public SubsystemResourceRegistrationXMLElement getSubsystemXMLElement() {
        ResourceXMLChoice sessionManagementChoice = this.factory.choice()
                .withCardinality(XMLCardinality.Unbounded.REQUIRED)
                .addElement(this.infinispanSessionManagementElement())
                .addElement(this.hotrodSessionManagementElement())
                .build();
        ResourceXMLChoice userManagementChoice = this.factory.choice()
                .withCardinality(XMLCardinality.Unbounded.REQUIRED)
                .addElement(this.userManagementElement(UserManagementResourceRegistration.INFINISPAN, InfinispanUserManagementResourceDefinitionRegistrar.CACHE_ATTRIBUTE_GROUP))
                .addElement(this.userManagementElement(UserManagementResourceRegistration.HOTROD, HotRodUserManagementResourceDefinitionRegistrar.CACHE_ATTRIBUTE_GROUP))
                .build();
        ResourceXMLChoice routingProviderChoice = this.factory.choice()
                .addElement(this.localRoutingElement())
                .addElement(this.infinispanRoutingElement())
                .build();
        ResourceXMLSequence content = this.factory.sequence()
                .addChoice(sessionManagementChoice)
                .addChoice(userManagementChoice)
                .addChoice(routingProviderChoice)
                .build();
        return this.factory.subsystemElement(DistributableWebSubsystemResourceDefinitionRegistrar.REGISTRATION)
                .addAttributes(List.of(DistributableWebSubsystemResourceDefinitionRegistrar.DEFAULT_SESSION_MANAGEMENT, DistributableWebSubsystemResourceDefinitionRegistrar.DEFAULT_USER_MANAGEMENT))
                .withContent(content)
                .build();
    }

    ResourceRegistrationXMLElement infinispanSessionManagementElement() {
        ResourceXMLChoice.Builder contentBuilder = this.affinityChoiceBuilder()
                .addElement(this.affinityElementBuilder(AffinityResourceRegistration.PRIMARY_OWNER).build())
                ;
        if (this.since(VERSION_2_0)) {
            contentBuilder.addElement(this.affinityElementBuilder(AffinityResourceRegistration.RANKED)
                    .addAttributes(ResourceDescriptor.stream(EnumSet.allOf(RankedAffinityResourceDefinitionRegistrar.Attribute.class)).toList())
                    .build());
        }
        return this.sessionManagementElementBuilder(SessionManagementResourceRegistration.INFINISPAN, InfinispanSessionManagementResourceDefinitionRegistrar.CACHE_ATTRIBUTE_GROUP)
                .withContent(contentBuilder.build())
                .build();
    }

    ResourceRegistrationXMLElement hotrodSessionManagementElement() {
        NamedResourceRegistrationXMLElement.Builder builder = this.sessionManagementElementBuilder(SessionManagementResourceRegistration.HOTROD, HotRodSessionManagementResourceDefinitionRegistrar.CACHE_ATTRIBUTE_GROUP);
        if (this.since(VERSION_4_0)) {
            builder.addAttribute(HotRodSessionManagementResourceDefinitionRegistrar.EXPIRATION_THREAD_POOL_SIZE);
        }
        return builder.withContent(this.affinityChoiceBuilder().build()).build();
    }

    NamedResourceRegistrationXMLElement.Builder sessionManagementElementBuilder(SessionManagementResourceRegistration registration, CacheConfigurationAttributeGroup cacheAttributes) {
        NamedResourceRegistrationXMLElement.Builder builder = this.factory.namedElement(registration)
                .addAttributes(cacheAttributes.getAttributes())
                .addAttribute(SessionManagementResourceDefinitionRegistrar.GRANULARITY)
                ;
        if (this.since(VERSION_3_0)) {
            builder.addAttribute(SessionManagementResourceDefinitionRegistrar.MARSHALLER);
        }
        return builder;
    }

    ResourceXMLChoice.Builder affinityChoiceBuilder() {
        return this.factory.choice()
                .addElement(this.affinityElementBuilder(AffinityResourceRegistration.NONE).withElementLocalName("no-affinity").build())
                .addElement(this.affinityElementBuilder(AffinityResourceRegistration.LOCAL).build())
                ;
    }

    SingletonResourceRegistrationXMLElement.Builder affinityElementBuilder(ResourceRegistration registration) {
        return this.factory.singletonElement(registration)
                .withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY)
                .withOperationKey(AffinityResourceRegistration.WILDCARD.getPathElement())
                ;
    }

    NamedResourceRegistrationXMLElement userManagementElement(UserManagementResourceRegistration registration, CacheConfigurationAttributeGroup cacheAttributes) {
        return this.factory.namedElement(registration)
                .addAttributes(cacheAttributes.getAttributes())
                .build();
    }

    ResourceRegistrationXMLElement infinispanRoutingElement() {
        return this.routingElementBuilder(RoutingProviderResourceRegistration.INFINISPAN)
                .addAttributes(InfinispanRoutingProviderResourceDefinitionRegistrar.CACHE_ATTRIBUTE_GROUP.getAttributes())
                .build();
    }

    ResourceRegistrationXMLElement localRoutingElement() {
        return this.routingElementBuilder(RoutingProviderResourceRegistration.LOCAL).build();
    }

    SingletonResourceRegistrationXMLElement.Builder routingElementBuilder(RoutingProviderResourceRegistration registration) {
        return this.factory.singletonElement(registration)
                .withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY)
                .withOperationKey(RoutingProviderResourceRegistration.WILDCARD.getPathElement())
                ;
    }
}

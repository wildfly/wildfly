/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.persistence.xml.NamedResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.ResourceXMLChoice;
import org.jboss.as.controller.persistence.xml.ResourceXMLParticleFactory;
import org.jboss.as.controller.persistence.xml.ResourceXMLSequence;
import org.jboss.as.controller.persistence.xml.SingletonResourceRegistrationXMLChoice;
import org.jboss.as.controller.persistence.xml.SingletonResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.SubsystemResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.SubsystemResourceXMLSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.controller.xml.XMLCardinality;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumerates the schema versions for the distributable-ejb subsystem.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public enum DistributableEjbSubsystemSchema implements SubsystemResourceXMLSchema<DistributableEjbSubsystemSchema> {

    VERSION_1_0(1, 0), // WildFly 27-35
    VERSION_2_0(2, 0), // WildFly 36-present
    VERSION_2_0_COMMUNITY(2, 0, Stability.COMMUNITY), // WildFly 39-present
    VERSION_3_0(3, 0), // WildFly 39-present
    ;
    static final DistributableEjbSubsystemSchema CURRENT = VERSION_3_0;

    private final VersionedNamespace<IntVersion, DistributableEjbSubsystemSchema> namespace;
    private final ResourceXMLParticleFactory factory = ResourceXMLParticleFactory.newInstance(this);

    DistributableEjbSubsystemSchema(int major, int minor) {
        this(major, minor, Stability.DEFAULT);
    }

    DistributableEjbSubsystemSchema(int major, int minor, Stability stability) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(DistributableEjbSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), stability, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, DistributableEjbSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public SubsystemResourceRegistrationXMLElement getSubsystemXMLElement() {
        SubsystemResourceRegistrationXMLElement.Builder builder = this.factory.subsystemElement(DistributableEjbSubsystemResourceDefinitionRegistrar.REGISTRATION);
        ResourceXMLSequence.Builder contentBuilder = this.factory.sequence();
        if (this.since(VERSION_2_0)) {
            contentBuilder.addElement(this.factory.element(this.factory.resolve("bean-management"))
                    .addAttribute(DistributableEjbSubsystemResourceDefinitionRegistrar.DEFAULT_BEAN_MANAGEMENT_PROVIDER)
                    .withContent(this.beanManagementChoice())
                    .build());
        } else {
            builder.addAttribute(DistributableEjbSubsystemResourceDefinitionRegistrar.DEFAULT_BEAN_MANAGEMENT_PROVIDER);
            builder.withLocalNames(Map.of(DistributableEjbSubsystemResourceDefinitionRegistrar.DEFAULT_BEAN_MANAGEMENT_PROVIDER, DistributableEjbSubsystemResourceDefinitionRegistrar.DEFAULT_BEAN_MANAGEMENT_PROVIDER.getName()));
            contentBuilder.addChoice(this.beanManagementChoice());
        }
        contentBuilder.addChoice(this.ejbClientServicesChoice());
        contentBuilder.addChoice(this.timerManagementChoice());
        return builder.withContent(contentBuilder.build())
                .build();
    }

    ResourceXMLChoice beanManagementChoice() {
        return this.factory.choice()
                .withCardinality(XMLCardinality.Unbounded.REQUIRED)
                .addElement(this.infinispanBeanManagementElement())
                .build();
    }

    NamedResourceRegistrationXMLElement infinispanBeanManagementElement() {
        return this.beanManagementElementBuilder(BeanManagementResourceRegistration.INFINISPAN)
                .addAttributes(InfinispanBeanManagementResourceDefinitionRegistrar.CACHE_ATTRIBUTE_GROUP.getAttributes())
                .build();
    }

    NamedResourceRegistrationXMLElement.Builder beanManagementElementBuilder(ResourceRegistration registration) {
        NamedResourceRegistrationXMLElement.Builder builder = this.factory.namedElement(registration)
                .addAttribute(BeanManagementResourceDefinitionRegistrar.MAX_ACTIVE_BEANS)
                ;
        if (this.since(VERSION_2_0_COMMUNITY)) {
            builder.addAttribute(BeanManagementResourceDefinitionRegistrar.IDLE_THRESHOLD);
        }
        return builder;
    }

    SingletonResourceRegistrationXMLChoice ejbClientServicesChoice() {
        return this.factory.singletonElementChoice()
                .addElement(this.localEjbClientServicesElement())
                .addElement(this.infinispanEjbClientServicesElement())
                .build();
    }

    SingletonResourceRegistrationXMLElement localEjbClientServicesElement() {
        return this.factory.singletonElement(EjbClientServicesProviderResourceRegistration.LOCAL)
                // change name of element to indicate multiple client services provided
                .withElementLocalName(this.since(VERSION_3_0) ? "local-ejb-client-services" : "local-client-mappings-registry")
                .build();
    }

    SingletonResourceRegistrationXMLElement infinispanEjbClientServicesElement() {
        return this.factory.singletonElement(EjbClientServicesProviderResourceRegistration.INFINISPAN)
                // change name of element to indicate multiple client services provided
                .withElementLocalName(this.since(VERSION_3_0) ? "infinispan-ejb-client-services" : "infinispan-client-mappings-registry")
                .addAttributes(InfinispanEjbClientServicesProviderResourceDefinitionRegistrar.CACHE_ATTRIBUTE_GROUP.getAttributes())
                .build();
    }

    ResourceXMLChoice timerManagementChoice() {
        return this.factory.choice()
                .withCardinality(XMLCardinality.Unbounded.OPTIONAL)
                .addElement(this.infinispanTimerManagementElement())
                .build();
    }
    NamedResourceRegistrationXMLElement infinispanTimerManagementElement() {
        NamedResourceRegistrationXMLElement.Builder builder = this.factory.namedElement(InfinispanTimerManagementResourceDefinitionRegistrar.REGISTRATION)
                .addAttributes(InfinispanTimerManagementResourceDefinitionRegistrar.CACHE_ATTRIBUTE_GROUP.getAttributes())
                .addAttributes(List.of(InfinispanTimerManagementResourceDefinitionRegistrar.MARSHALLER, InfinispanTimerManagementResourceDefinitionRegistrar.MAX_ACTIVE_TIMERS));
        if (this.since(VERSION_2_0_COMMUNITY)) {
            builder.addAttribute(InfinispanTimerManagementResourceDefinitionRegistrar.IDLE_THRESHOLD);
        }
        return builder.build();
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.singleton;

import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.persistence.xml.ResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.ResourceXMLAll;
import org.jboss.as.controller.persistence.xml.ResourceXMLChoice;
import org.jboss.as.controller.persistence.xml.ResourceXMLElement;
import org.jboss.as.controller.persistence.xml.ResourceXMLElementLocalName;
import org.jboss.as.controller.persistence.xml.ResourceXMLParticleFactory;
import org.jboss.as.controller.persistence.xml.SingletonResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.SubsystemResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.SubsystemResourceXMLSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.controller.xml.XMLCardinality;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumeration of supported subsystem schemas.
 * @author Paul Ferraro
 */
public enum SingletonSubsystemSchema implements SubsystemResourceXMLSchema<SingletonSubsystemSchema> {

    VERSION_1_0(1, 0),
    ;
    static final SingletonSubsystemSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, SingletonSubsystemSchema> namespace;
    private final ResourceXMLParticleFactory factory = ResourceXMLParticleFactory.newInstance(this);

    SingletonSubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(SingletonSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, SingletonSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public SubsystemResourceRegistrationXMLElement getSubsystemXMLElement() {
        ResourceXMLElement singletonPoliciesElement = this.factory.element(this.factory.resolve("singleton-policies"))
                .addAttribute(SingletonSubsystemResourceDefinitionRegistrar.DEFAULT_SERVICE_TARGET_FACTORY)
                .withContent(this.singletonPolicyChoice())
                .build();

        ResourceXMLAll content = this.factory.all()
                .addElement(singletonPoliciesElement)
                .build();

        return this.factory.subsystemElement(SingletonSubsystemResourceDefinitionRegistrar.REGISTRATION)
                .withContent(content)
                .build();
    }

    private ResourceXMLChoice singletonPolicyChoice() {
        return this.factory.choice()
                .withCardinality(XMLCardinality.Unbounded.REQUIRED)
                .addElement(this.singletonPolicyElement())
                .build();
    }

    private ResourceRegistrationXMLElement singletonPolicyElement() {
        return this.factory.namedElement(SingletonPolicyResourceDefinitionRegistrar.REGISTRATION)
                .addAttributes(SingletonPolicyResourceDefinitionRegistrar.CACHE_ATTRIBUTE_GROUP.getAttributes())
                .addAttribute(SingletonPolicyResourceDefinitionRegistrar.QUORUM)
                .withContent(this.electionPolicyChoice())
                .build();
    }

    private ResourceXMLChoice electionPolicyChoice() {
        return this.factory.singletonElementChoice()
                .addElement(this.simpleElectionPolicyElement())
                .addElement(this.randomElectionPolicyElement())
                .build();
    }

    private SingletonResourceRegistrationXMLElement simpleElectionPolicyElement() {
        return this.electionPolicyElementBuilder(ElectionPolicyResourceRegistration.SIMPLE)
                .addAttribute(SimpleElectionPolicyResourceDefinitionRegistrar.POSITION)
                .build();
    }

    private SingletonResourceRegistrationXMLElement randomElectionPolicyElement() {
        return this.electionPolicyElementBuilder(ElectionPolicyResourceRegistration.RANDOM)
                .build();
    }

    private SingletonResourceRegistrationXMLElement.Builder electionPolicyElementBuilder(ResourceRegistration registration) {
        return this.factory.singletonElement(registration)
                .withOperationKey(ElectionPolicyResourceRegistration.WILDCARD.getPathElement())
                .withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY)
                .withContent(this.preferencesChoice())
                ;
    }

    private ResourceXMLChoice preferencesChoice() {
        return this.factory.choice()
                .withCardinality(XMLCardinality.Single.OPTIONAL)
                .addElement(ElectionPolicyResourceDefinitionRegistrar.NAME_PREFERENCES)
                .addElement(ElectionPolicyResourceDefinitionRegistrar.SOCKET_BINDING_PREFERENCES)
                .build();
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton.deployment;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.xml.IntVersionSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.controller.xml.XMLComponentFactory;
import org.jboss.as.controller.xml.XMLElement;
import org.jboss.as.controller.xml.XMLElementSchema;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.jbossallxml.JBossAllSchema;
import org.jboss.staxmapper.IntVersion;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Enumerates the singleton deployment configuration schemas.
 * @author Paul Ferraro
 */
public enum SingletonDeploymentSchema implements XMLElementSchema<SingletonDeploymentSchema, MutableSingletonDeploymentConfiguration>, JBossAllSchema<SingletonDeploymentSchema, SingletonDeploymentConfiguration> {

    VERSION_1_0(1, 0),
    ;
    public static final SingletonDeploymentSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, SingletonDeploymentSchema> namespace;
    private final XMLComponentFactory<MutableSingletonDeploymentConfiguration, Void> factory = XMLComponentFactory.newInstance(this);

    SingletonDeploymentSchema(int major, int minor) {
        this.namespace = IntVersionSchema.createURN(List.of(IntVersionSchema.JBOSS_IDENTIFIER, this.getLocalName()), new IntVersion(major, minor));
    }

    @Override
    public String getLocalName() {
        return "singleton-deployment";
    }

    @Override
    public VersionedNamespace<IntVersion, SingletonDeploymentSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, MutableSingletonDeploymentConfiguration configuration) throws XMLStreamException {
        XMLElement<MutableSingletonDeploymentConfiguration, Void> element = this.factory.element(this.getQualifiedName())
                .addAttribute(this.factory.attribute(this.resolve("policy")).withConsumer(MutableSingletonDeploymentConfiguration::setPolicy).build())
                .build();
        element.getReader().readElement(reader, configuration);
    }

    @Override
    public SingletonDeploymentConfiguration parse(XMLExtendedStreamReader reader, DeploymentUnit unit) throws XMLStreamException {
        MutableSingletonDeploymentConfiguration configuration = new MutableSingletonDeploymentConfiguration(unit);
        this.readElement(reader, configuration);
        return configuration;
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.processor;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.ee.structure.JBossDescriptorPropertyReplacement;
import org.jboss.as.jpa.config.JPADeploymentSettings;
import org.jboss.as.jpa.jbossjpaparser.JBossJPAParser;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXMLParser;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * JPAJarJBossAllParser
 *
 * @author Scott Marlow
 */
public class JPAJarJBossAllParser implements JBossAllXMLParser<JPADeploymentSettings> {

    public static final AttachmentKey<JPADeploymentSettings> ATTACHMENT_KEY = AttachmentKey.create(JPADeploymentSettings.class);
    public static final QName ROOT_ELEMENT = new QName("http://www.jboss.com/xml/ns/javaee", "jboss-jpa");

    @Override
    public JPADeploymentSettings parse(final XMLExtendedStreamReader reader, final DeploymentUnit deploymentUnit) throws XMLStreamException {
        return JBossJPAParser.parser(reader, JBossDescriptorPropertyReplacement.propertyReplacer(deploymentUnit));
    }

}

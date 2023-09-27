/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.ee.structure.JBossDescriptorPropertyReplacement;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXMLParser;
import org.jboss.metadata.parser.jbossweb.JBossWebMetaDataParser;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * The app client handler for jboss-all.xml
 *
 * @author Stuart Douglas
 */
public class WebJBossAllParser implements JBossAllXMLParser<JBossWebMetaData> {

    public static final AttachmentKey<JBossWebMetaData> ATTACHMENT_KEY = AttachmentKey.create(JBossWebMetaData.class);

    public static final QName ROOT_ELEMENT = new QName("http://www.jboss.com/xml/ns/javaee", "jboss-web");

    @Override
    public JBossWebMetaData parse(final XMLExtendedStreamReader reader, final DeploymentUnit deploymentUnit) throws XMLStreamException {
        return JBossWebMetaDataParser.parse(reader, JBossDescriptorPropertyReplacement.propertyReplacer(deploymentUnit));
    }

}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.structure;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXMLParser;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.metadata.ear.parser.jboss.JBossAppMetaDataParser;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * The app client handler for jboss-all.xml
 *
 * @author Stuart Douglas
 */
public class AppJBossAllParser implements JBossAllXMLParser<JBossAppMetaData> {

    public static final AttachmentKey<JBossAppMetaData> ATTACHMENT_KEY = AttachmentKey.create(JBossAppMetaData.class);

    public static final QName ROOT_ELEMENT = new QName("http://www.jboss.com/xml/ns/javaee", "jboss-app");

    @Override
    public JBossAppMetaData parse(final XMLExtendedStreamReader reader, final DeploymentUnit deploymentUnit) throws XMLStreamException {
        return JBossAppMetaDataParser.INSTANCE.parse(reader, JBossDescriptorPropertyReplacement.propertyReplacer(deploymentUnit));
    }

}

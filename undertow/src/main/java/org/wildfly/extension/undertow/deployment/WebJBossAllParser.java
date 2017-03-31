/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

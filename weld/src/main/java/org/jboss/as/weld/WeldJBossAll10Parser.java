/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld;

import static org.jboss.as.weld.WeldResourceDefinition.NON_PORTABLE_MODE_ATTRIBUTE_NAME;
import static org.jboss.as.weld.WeldResourceDefinition.REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE_NAME;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXMLParser;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parses <code>jboss-all.xml</code> and creates {@link WeldJBossAllConfiguration} holding the parsed configuration.
 *
 * @author Jozef Hartinger
 *
 */
class WeldJBossAll10Parser extends AbstractWeldJBossAllParser implements JBossAllXMLParser<WeldJBossAllConfiguration> {

    public static final String NAMESPACE = "urn:jboss:weld:1.0";
    public static final QName ROOT_ELEMENT = new QName(NAMESPACE, "weld");
    public static final WeldJBossAll10Parser INSTANCE = new WeldJBossAll10Parser();

    private WeldJBossAll10Parser() {
    }

    @Override
    public WeldJBossAllConfiguration parse(XMLExtendedStreamReader reader, DeploymentUnit deploymentUnit) throws XMLStreamException {
        Boolean requireBeanDescriptor = getAttributeAsBoolean(reader, REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE_NAME);
        Boolean nonPortableMode = getAttributeAsBoolean(reader, NON_PORTABLE_MODE_ATTRIBUTE_NAME);
        return new WeldJBossAllConfiguration(requireBeanDescriptor, nonPortableMode, false);
    }

}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld;

import static org.jboss.as.weld.WeldResourceDefinition.DEVELOPMENT_MODE_ATTRIBUTE_NAME;
import static org.jboss.as.weld.WeldResourceDefinition.LEGACY_EMPTY_BEANS_XML_TREATMENT_ATTRIBUTE_NAME;
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
class WeldJBossAll11Parser extends AbstractWeldJBossAllParser implements JBossAllXMLParser<WeldJBossAllConfiguration> {

    public static final String NAMESPACE = "urn:jboss:weld:1.1";
    public static final QName ROOT_ELEMENT = new QName(NAMESPACE, "weld");
    public static final WeldJBossAll11Parser INSTANCE = new WeldJBossAll11Parser();

    private WeldJBossAll11Parser() {
    }

    @Override
    public WeldJBossAllConfiguration parse(XMLExtendedStreamReader reader, DeploymentUnit deploymentUnit) throws XMLStreamException {
        Boolean requireBeanDescriptor = getAttributeAsBoolean(reader, REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE_NAME);
        Boolean nonPortableMode = getAttributeAsBoolean(reader, NON_PORTABLE_MODE_ATTRIBUTE_NAME);
        Boolean developmentMode = getAttributeAsBoolean(reader, DEVELOPMENT_MODE_ATTRIBUTE_NAME);
        Boolean legacyEmptyBeansXmlTreatment = getAttributeAsBoolean(reader, LEGACY_EMPTY_BEANS_XML_TREATMENT_ATTRIBUTE_NAME);
        super.parseWeldElement(reader);
        return new WeldJBossAllConfiguration(requireBeanDescriptor, nonPortableMode, developmentMode, legacyEmptyBeansXmlTreatment);    }

}

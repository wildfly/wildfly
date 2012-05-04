/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.service;

import java.net.URL;
import org.jboss.as.service.descriptor.JBossServiceAttributeConfig;
import org.jboss.as.service.descriptor.JBossServiceConfig;
import org.jboss.as.service.descriptor.JBossServiceConstructorConfig;
import org.jboss.as.service.descriptor.JBossServiceDependencyConfig;
import org.jboss.as.service.descriptor.JBossServiceXmlDescriptor;
import org.jboss.as.service.descriptor.JBossServiceXmlDescriptorParser;
import org.jboss.as.model.ParseResult;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test to verify the {@code JBossServiceXmlDescriptorParser} implementation.
 *
 * @author John E. Bailey
 */
public class JBossServiceXmlDescriptorParserTestCase {

    private JBossServiceXmlDescriptorParser parser;
    private XMLMapper xmlMapper;
    private XMLInputFactory inputFactory;

    @Before
    public void setupParser() throws Exception {
        parser = new JBossServiceXmlDescriptorParser(propertyReplacer);
        xmlMapper = XMLMapper.Factory.create();
        xmlMapper.registerRootElement(new QName(JBossServiceXmlDescriptorParser.NAMESPACE, "server"), parser);
        inputFactory = XMLInputFactory.newInstance();
    }

    @Test
    public void testParse() throws Exception {
        final File testXmlFile = getResourceFile(JBossServiceXmlDescriptorParserTestCase.class, "/test/serviceXmlDeployment.jar/META-INF/jboss-service.xml");
        final ParseResult<JBossServiceXmlDescriptor> jBossServiceXmlDescriptorParseResult = new ParseResult<JBossServiceXmlDescriptor>();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(testXmlFile);
            final XMLStreamReader reader = inputFactory.createXMLStreamReader(inputStream);
            xmlMapper.parseDocument(jBossServiceXmlDescriptorParseResult, reader);
        } finally {
            if (inputStream != null) inputStream.close();
        }
        final JBossServiceXmlDescriptor xmlDescriptor = jBossServiceXmlDescriptorParseResult.getResult();
        assertNotNull(xmlDescriptor);
        final List<JBossServiceConfig> serviceConfigs = xmlDescriptor.getServiceConfigs();
        assertEquals(3, serviceConfigs.size());

        for (JBossServiceConfig jBossServiceConfig : serviceConfigs) {
            assertEquals("org.jboss.as.service.LegacyService", jBossServiceConfig.getCode());
            if (jBossServiceConfig.getName().equals("jboss.test.service")) {
                final JBossServiceConstructorConfig constructorConfig = jBossServiceConfig.getConstructorConfig();
                assertNotNull(constructorConfig);
                final JBossServiceConstructorConfig.Argument[] arguments = constructorConfig.getArguments();
                assertEquals(1, arguments.length);
                assertEquals(String.class.getName(), arguments[0].getType());
                assertEquals("Test Value", arguments[0].getValue());
            } else if (jBossServiceConfig.getName().equals("jboss.test.service.second")) {
                assertNull(jBossServiceConfig.getConstructorConfig());
                final JBossServiceDependencyConfig[] dependencyConfigs = jBossServiceConfig.getDependencyConfigs();
                assertEquals(1, dependencyConfigs.length);
                assertEquals("jboss.test.service", dependencyConfigs[0].getDependencyName());
                assertEquals("other", dependencyConfigs[0].getOptionalAttributeName());
                final JBossServiceAttributeConfig[] attributeConfigs = jBossServiceConfig.getAttributeConfigs();
                assertEquals(1, attributeConfigs.length);
                assertEquals("somethingElse", attributeConfigs[0].getName());
                assertNull(attributeConfigs[0].getInject());
                final JBossServiceAttributeConfig.ValueFactory valueFactory = attributeConfigs[0].getValueFactory();
                assertNotNull(valueFactory);
                assertEquals("jboss.test.service", valueFactory.getBeanName());
                assertEquals("appendSomethingElse", valueFactory.getMethodName());
                final JBossServiceAttributeConfig.ValueFactoryParameter[] parameters = valueFactory.getParameters();
                assertEquals(1, parameters.length);
                assertEquals("java.lang.String", parameters[0].getType());
                assertEquals("more value", parameters[0].getValue());
            } else if (jBossServiceConfig.getName().equals("jboss.test.service.third")) {
                assertNull(jBossServiceConfig.getConstructorConfig());

                final JBossServiceAttributeConfig[] attributeConfigs = jBossServiceConfig.getAttributeConfigs();
                assertEquals(2, attributeConfigs.length);
                assertEquals("other", attributeConfigs[0].getName());
                assertNull(attributeConfigs[0].getValueFactory());

                final JBossServiceAttributeConfig.Inject inject = attributeConfigs[0].getInject();
                assertNotNull(inject);
                assertEquals("jboss.test.service.second", inject.getBeanName());
                assertEquals("other", inject.getPropertyName());

                assertEquals("somethingElse", attributeConfigs[1].getName());
                assertNull(attributeConfigs[1].getValueFactory());
                assertNull(attributeConfigs[1].getInject());
                assertEquals("Another test value", attributeConfigs[1].getValue());
            }
        }
    }

    protected URL getResource(final Class<?> testClass, final String path) throws Exception {
        return testClass.getResource(path);
    }

    protected File getResourceFile(final Class<?> testClass, final String path) throws Exception {
        return new File(getResource(testClass, path).toURI());
    }

}

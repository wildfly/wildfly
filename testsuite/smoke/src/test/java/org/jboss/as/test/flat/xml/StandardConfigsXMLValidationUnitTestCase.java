/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.flat.xml;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * A XSDValidationUnitTestCase.
 *
 * @author Brian Stansberry
 * @version $Revision: 1.1 $
 */
public class StandardConfigsXMLValidationUnitTestCase extends AbstractValidationUnitTest {
    private static Source[] SCHEMAS;

    @BeforeClass
    public static void setUp() {
        final List<Source> sources = new LinkedList<Source>();
        for (File file : jbossSchemaFiles()) {
            sources.add(new StreamSource(file));
        }
        SCHEMAS = sources.toArray(new StreamSource[0]);
    }

    @Test
    public void testHost() throws Exception {
        parseXml("domain/configuration/host.xml");
    }

    @Test
    public void testDomain() throws Exception {
        parseXml("domain/configuration/domain.xml");
    }

    @Test
    public void testStandalone() throws Exception {
        parseXml("standalone/configuration/standalone.xml");
    }

    @Test
    public void testStandalonePreview() throws Exception {
        parseXml("standalone/configuration/standalone-preview.xml");
    }

    private void parseXml(String xmlName) throws ParserConfigurationException, SAXException, IOException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setErrorHandler(new ErrorHandlerImpl());
        schemaFactory.setResourceResolver(DEFAULT_RESOURCE_RESOLVER);
        Schema schema = schemaFactory.newSchema(SCHEMAS);
        Validator validator = schema.newValidator();
        validator.setFeature("http://apache.org/xml/features/validation/schema", true);
        validator.setResourceResolver(DEFAULT_RESOURCE_RESOLVER);
        validator.validate(new StreamSource(getXmlFile(xmlName)));
    }

    private File getXmlFile(String xmlName) throws MalformedURLException {
        if (baseDir() == null)
            Assert.fail("Server not built");

        return new File(baseDir(), xmlName);
    }
}

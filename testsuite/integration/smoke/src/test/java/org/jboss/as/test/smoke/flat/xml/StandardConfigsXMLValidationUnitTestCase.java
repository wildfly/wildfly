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
package org.jboss.as.test.smoke.flat.xml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import junit.framework.Assert;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.python.google.common.io.LineReader;
import org.xml.sax.SAXException;

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

    private File tmpFile;

    @After
    public void cleanUp() {
        if (tmpFile != null) {
            if (!tmpFile.delete()) {
                tmpFile.deleteOnExit();
            }
        }
    }

    @Test
    public void testHost() throws Exception {
        parseXml("domain/configuration/host.xml");
    }

    @Test
    public void testHostSlave() throws Exception {
        parseXml("domain/configuration/host-slave.xml");
    }

    @Test
    public void testHostMaster() throws Exception {
        parseXml("domain/configuration/host-master.xml");
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
    public void testStandaloneHA() throws Exception {
        parseXml("standalone/configuration/standalone-ha.xml");
    }

    @Test
    public void testStandaloneFull() throws Exception {
        parseXml("standalone/configuration/standalone-full.xml");
    }

    @Test
    public void testDomainJTS() throws Exception {
        parseXml("docs/examples/configs/domain-jts.xml");
    }

    @Test
    public void testDomainOSGiOnly() throws Exception {
        parseXml("docs/examples/configs/domain-osgi-only.xml");
    }

    @Test
    public void testHornetQColocated() throws Exception {
        parseXml("docs/examples/configs/standalone-hornetq-colocated.xml");
    }

    @Test
    public void testStandaloneJTS() throws Exception {
        parseXml("docs/examples/configs/standalone-jts.xml");
    }

    @Test
    public void testStandaloneOSGiOnly() throws Exception {
        parseXml("docs/examples/configs/standalone-osgi-only.xml");
    }

    @Test
    public void testStandaloneMinimalistic() throws Exception {
        parseXml("docs/examples/configs/standalone-minimalistic.xml");
    }

    @Test
    public void testStandaloneXTS() throws Exception {
        parseXml("docs/examples/configs/standalone-xts.xml");
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

    private File getXmlFile(String xmlName) throws IOException {

        // Copy the input file to tmp, replacing system prop expressions on non-string fields
        // so they don't cause validation failures
        // TODO we should just pass an IS to Validator
        final File tmp = File.createTempFile(getClass().getSimpleName(), "xml");
        tmp.deleteOnExit();
        File target = new File(getBaseDir(), xmlName);
        BufferedReader reader = new BufferedReader(new FileReader(target));
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(tmp));
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(fixExpressions(line));
            }
        } finally {
            reader.close();
            if (writer != null) {
                writer.close();
            }
        }
        return tmp;
    }

    private static String fixExpressions(String line) {
        String result = line.replace("${jboss.management.native.port:9999}", "9999");
        result = result.replace("${jboss.management.http.port:9990}", "9990");
        result = result.replace("${jboss.management.https.port:9443}", "9443");
        result = result.replace("${jboss.domain.master.port:9999}", "9999");
        result = result.replace("${jboss.socket.binding.port-offset:0}", "0");
        return result;
    }
}

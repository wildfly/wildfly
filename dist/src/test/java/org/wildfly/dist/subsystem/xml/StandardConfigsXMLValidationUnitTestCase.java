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

package org.wildfly.dist.subsystem.xml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
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
        for (File file : jbossSchemaFiles(true)) {
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

    //TODO Leave commented out until domain-jts.xml is definitely removed from the configuration
//    @Test
//    public void testDomainJTS() throws Exception {
//        parseXml("docs/examples/configs/domain-jts.xml");
//    }
//
    @Test
    public void testStandaloneEC2HA() throws Exception {
        parseXml("docs/examples/configs/standalone-ec2-ha.xml");
    }

    @Test
    public void testStandaloneEC2FullHA() throws Exception {
        parseXml("docs/examples/configs/standalone-ec2-full-ha.xml");

    }
    @Test
    public void testStandaloneGossipHA() throws Exception {
        parseXml("docs/examples/configs/standalone-gossip-ha.xml");
    }

    @Test
    public void testStandaloneGossipFullHA() throws Exception {
        parseXml("docs/examples/configs/standalone-gossip-full-ha.xml");
    }

    @Test
    public void testStandaloneJTS() throws Exception {
        parseXml("docs/examples/configs/standalone-jts.xml");
    }

    @Test
    public void testStandaloneMinimalistic() throws Exception {
        parseXml("docs/examples/configs/standalone-minimalistic.xml");
    }

    @Test
    public void testStandalonePicketLink() throws Exception {
        parseXml("docs/examples/configs/standalone-picketlink.xml");
    }

    @Test
    public void testStandaloneXTS() throws Exception {
        parseXml("docs/examples/configs/standalone-xts.xml");
    }

    @Test
    public void testStandaloneRTS() throws Exception {
        parseXml("docs/examples/configs/standalone-rts.xml");
    }

    @Test
    public void testStandaloneGenericJMS() throws Exception {
        parseXml("docs/examples/configs/standalone-genericjms.xml");
    }

    private void parseXml(String xmlName) throws ParserConfigurationException, SAXException, IOException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setErrorHandler(new ErrorHandlerImpl());
        schemaFactory.setResourceResolver(DEFAULT_RESOURCE_RESOLVER);
        Schema schema = schemaFactory.newSchema(SCHEMAS);
        Validator validator = schema.newValidator();
        validator.setErrorHandler(new ErrorHandlerImpl());
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
        result = result.replace("${jboss.management.https.port:9993}", "9993");
        result = result.replace("${jboss.domain.master.protocol:remote}", "remote");
        result = result.replace("${jboss.domain.master.protocol:remote+http}", "remote+http");
        result = result.replace("${jboss.domain.master.port:9999}", "9999");
        result = result.replace("${jboss.domain.master.port:9990}", "9990");
        result = result.replace("${jboss.messaging.group.port:9876}", "9876");
        result = result.replace("${jboss.socket.binding.port-offset:0}", "0");
        result = result.replace("${jboss.http.port:8080}", "8080");
        result = result.replace("${jboss.https.port:8443}", "8443");
        result = result.replace("${jboss.remoting.port:4447}", "4447");
        result = result.replace("${jboss.ajp.port:8009}", "8009");
        result = result.replace("${jboss.mcmp.port:8090}", "8090");
        result = result.replace("${jboss.deployment.scanner.rollback.on.failure:false}", "false");
        return result;
    }
}

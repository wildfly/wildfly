/*
 * Copyright 2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  *
 * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.wildfly.test.integration.vdx.standalone;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.commands.foundation.offline.xml.GroovyXmlTransform;
import org.wildfly.extras.creaper.commands.foundation.offline.xml.Subtree;
import org.wildfly.extras.creaper.core.offline.OfflineCommand;
import org.wildfly.test.integration.vdx.TestBase;
import org.wildfly.test.integration.vdx.category.StandaloneTests;
import org.wildfly.test.integration.vdx.transformations.DoNothing;
import org.wildfly.test.integration.vdx.utils.server.Server;
import org.wildfly.test.integration.vdx.utils.server.ServerConfig;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Scanner;

/**
 *
 * Tests for invalid characters in configuration files
 *
 * Created by rsvoboda on 1/18/17.
 */

@RunAsClient
@RunWith(Arquillian.class)
@Category(StandaloneTests.class)
public class InvalidCharactersTestCase extends TestBase {
    private static final Path standaloneXml = Server.CONFIGURATION_PATH.resolve("standalone.xml");
    private static final String STANDALONE_MISSING_START_OF_ELEMENT_XML = "standalone-missingStartOfElement.xml";
    private static final String STANDALONE_SPACE_IN_XML_DECLARATION_XML = "standalone-spaceInXmlDeclaration.xml";
    private static final String STANDALONE_MISSING_LAST_QUOTE_XML = "standalone-missingLastQuote.xml";
    private static final Path missingStartOfElementStandaloneXml = Server.CONFIGURATION_PATH.resolve(STANDALONE_MISSING_START_OF_ELEMENT_XML);
    private static final Path spaceInXmlDeclarationStandaloneXml = Server.CONFIGURATION_PATH.resolve(STANDALONE_SPACE_IN_XML_DECLARATION_XML);
    private static final Path missingLastQuoteStandaloneXml = Server.CONFIGURATION_PATH.resolve(STANDALONE_MISSING_LAST_QUOTE_XML);

    private static final String missingStartOfElement =
            "        <subsystem xmlns=\"urn:jboss:domain:webservices:2.0\">\n"
                    + "            <wsdl-host>${jboss.bind.address:127.0.0.1}</wsdl-host>\n"
                    + "            modify-wsdl-address>true</modify-wsdl-address>\n" //  <-- on this line is the missing start <
                    + "            <endpoint-config name=\"Standard-Endpoint-Config\"/>\n"
                    + "            <endpoint-config name=\"Recording-Endpoint-Config\">\n"
                    + "                <pre-handler-chain name=\"recording-handlers\" protocol-bindings=\"##SOAP11_HTTP ##SOAP11_HTTP_MTOM ##SOAP12_HTTP ##SOAP12_HTTP_MTOM\">\n"
                    + "                    <handler name=\"RecordingHandler\" class=\"org.jboss.ws.common.invocation.RecordingServerHandler\"/>\n"
                    + "                </pre-handler-chain>\n"
                    + "            </endpoint-config>\n"
                    + "            <client-config name=\"Standard-Client-Config\"/>\n"
                    + "        </subsystem>";

    private static final String missingLastQuote =
            "        <subsystem xmlns=\"urn:jboss:domain:webservices:2.0\">\n"
                    + "            <wsdl-host>${jboss.bind.address:127.0.0.1}</wsdl-host>\n"
                    + "            <modify-wsdl-address>true</modify-wsdl-address>\n"
                    + "            <endpoint-config name=\"Standard-Endpoint-Config/>\n" //  <-- here is the missing end quote
                    + "            <endpoint-config name=\"Recording-Endpoint-Config\">\n"
                    + "                <pre-handler-chain name=\"recording-handlers\" protocol-bindings=\"##SOAP11_HTTP ##SOAP11_HTTP_MTOM ##SOAP12_HTTP ##SOAP12_HTTP_MTOM\">\n"
                    + "                    <handler name=\"RecordingHandler\" class=\"org.jboss.ws.common.invocation.RecordingServerHandler\"/>\n"
                    + "                </pre-handler-chain>\n"
                    + "            </endpoint-config>\n"
                    + "            <client-config name=\"Standard-Client-Config\"/>\n"
                    + "        </subsystem>";

    private String elementWithNationalCharacters = "<řebříček>obecný</řebříček>";

    @BeforeClass
    public static void prepareBrokenConfiguration() throws IOException {
        try (Scanner scanner = new Scanner(standaloneXml);
            PrintWriter missingStartOfElementStandaloneXmlPrintWriter = new PrintWriter(missingStartOfElementStandaloneXml.toFile());
            PrintWriter spaceInXmlDeclarationStandaloneXmlPrintWriter = new PrintWriter(spaceInXmlDeclarationStandaloneXml.toFile());
            PrintWriter missingLastQuoteStandaloneXmlPrintWriter = new PrintWriter(missingLastQuoteStandaloneXml.toFile())) {

            boolean inWSSubsystem = false;
            boolean wasInFirstLine = false;

            while (scanner.hasNext()) {
                String line = scanner.nextLine();

                if (wasInFirstLine) {
                    spaceInXmlDeclarationStandaloneXmlPrintWriter.println(line);
                } else {
                    wasInFirstLine = true;
                    spaceInXmlDeclarationStandaloneXmlPrintWriter.println(" " + line);
                }

                if (line.contains("<subsystem xmlns=\"urn:jboss:domain:webservices")) {
                    inWSSubsystem = true;

                } else if (!inWSSubsystem) {
                    missingStartOfElementStandaloneXmlPrintWriter.println(line);
                    missingLastQuoteStandaloneXmlPrintWriter.println(line);

                } else if (line.contains("</subsystem>")) {
                    missingStartOfElementStandaloneXmlPrintWriter.println(missingStartOfElement);
                    missingLastQuoteStandaloneXmlPrintWriter.println(missingLastQuote);
                    inWSSubsystem = false;
                }
            }
        }
    }

    /*
     * Add space before xml declaration - <space><?xml version='1.0' encoding='UTF-8'?>
     */
    @Test
    @ServerConfig(configuration = STANDALONE_SPACE_IN_XML_DECLARATION_XML)
    public void spaceInXmlDeclaration()throws Exception {
        container().tryStartAndWaitForFail();
        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, " ^^^^ Illegal processing instruction target (\"xml\"); xml (case insensitive)");
        assertContains(errorLog, "is reserved by the specs");
    }

    /*
     * There is bad character in xml (for example < is missing)
     */
    @Test
    @ServerConfig(configuration = STANDALONE_MISSING_START_OF_ELEMENT_XML)
    public void missingStartOfElement()throws Exception {
        container().tryStartAndWaitForFail();
        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, " modify-wsdl-address>true</modify-wsdl-address>");
        assertContains(errorLog, "^^^^ Received non-all-whitespace CHARACTERS or CDATA event in nextTag()");

    }

    /*
     * There is missing last quote " in <element name="foobar>
     */
    @Test
    @ServerConfig(configuration = STANDALONE_MISSING_LAST_QUOTE_XML)
    public void missingLastQuoteStandaloneXml()throws Exception {
        container().tryStartAndWaitForFail();
        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "<endpoint-config name=\"Standard-Endpoint-Config/>");
        assertContains(errorLog, "^^^^ Unexpected character '<' (code 60) in attribute value");
    }

    /*
     * Element with national characters - e.g. <řebříček>obecný</řebříček>
     */
    @Test
    @ServerConfig()
    public void elementWithNationalCharacters()throws Exception {
        container().tryStartAndWaitForFail(
                (OfflineCommand) ctx -> ctx.client.apply(GroovyXmlTransform.of(DoNothing.class, "AddElement.groovy")
                        .subtree("path", Subtree.subsystem("webservices")).parameter("elementXml", elementWithNationalCharacters)
                        .build()));
        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "isn't an allowed element here");
        assertContains(errorLog, "Elements allowed here are:");
    }


}

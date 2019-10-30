/*
 * Copyright 2016 Red Hat, Inc, and individual contributors.
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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.commands.foundation.offline.xml.GroovyXmlTransform;
import org.wildfly.extras.creaper.commands.foundation.offline.xml.Subtree;
import org.wildfly.extras.creaper.core.offline.OfflineCommand;
import org.wildfly.test.integration.vdx.TestBase;
import org.wildfly.test.integration.vdx.category.StandaloneTests;
import org.wildfly.test.integration.vdx.transformations.DoNothing;
import org.wildfly.test.integration.vdx.utils.server.ServerConfig;

/**
 *
 * Created by rsvoboda on 11/30/16.
 */

@RunAsClient
@RunWith(Arquillian.class)
@Category(StandaloneTests.class)
public class JBossWSTestCase extends TestBase {

    /*
     * <modify-wsdl-address /> instead of <modify-wsdl-address>true</modify-wsdl-address>
     */
    @Test
    @ServerConfig(configuration = "standalone.xml", xmlTransformationGroovy = "webservices/AddModifyWsdlAddressElementWithNoValue.groovy",
            subtreeName = "webservices", subsystemName = "webservices")
    public void modifyWsdlAddressElementWithNoValue()throws Exception {
        container().tryStartAndWaitForFail();

        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "OPVDX001: Validation error in standalone.xml");
        assertContains(errorLog, "<modify-wsdl-address/>");
        assertContains(errorLog, " ^^^^ Wrong type for 'modify-wsdl-address'. Expected [BOOLEAN] but was");
        assertContains(errorLog, "STRING");
    }

    /*
     * <mmodify-wsdl-address>true</mmodify-wsdl-address> instead of <modify-wsdl-address>true</modify-wsdl-address>
     */
    @Test
    @ServerConfig(configuration = "standalone.xml", xmlTransformationGroovy = "webservices/AddIncorrectlyNamedModifyWsdlAddressElement.groovy",
            subtreeName = "webservices", subsystemName = "webservices")
    public void incorrectlyNamedModifyWsdlAddressElement()throws Exception {
        container().tryStartAndWaitForFail();

        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "OPVDX001: Validation error in standalone.xml");
        assertContains(errorLog, "<mmodify-wsdl-address>true</mmodify-wsdl-address>");
        assertContains(errorLog, "^^^^ 'mmodify-wsdl-address' isn't an allowed element here");
        assertContains(errorLog, " Did you mean 'modify-wsdl-address'?");
        assertContains(errorLog, "Elements allowed here are:");
        assertContains(errorLog, "client-config");
        assertContains(errorLog, "wsdl-path-rewrite-rule");
        assertContains(errorLog, "endpoint-config");
        assertContains(errorLog, "wsdl-port");
        assertContains(errorLog, "modify-wsdl-address");
        assertContains(errorLog, "wsdl-secure-port");
        assertContains(errorLog, "wsdl-host");
        assertContains(errorLog, "wsdl-uri-scheme");
    }

    /*
     * <modify-wsdl-address>ttrue</modify-wsdl-address> instead of <modify-wsdl-address>true</modify-wsdl-address>
     */
    @Test
    @ServerConfig(configuration = "standalone.xml", xmlTransformationGroovy = "webservices/AddModifyWsdlAddressElementWithIncorrectValue.groovy",
            subtreeName = "webservices", subsystemName = "webservices")
    public void incorrectValueOfModifyWsdlAddressElement()throws Exception {
        container().tryStartAndWaitForFail();

        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "OPVDX001: Validation error in standalone.xml");
        assertContains(errorLog, "<modify-wsdl-address>ttrue</modify-wsdl-address>");
        assertContains(errorLog, " ^^^^ Wrong type for 'modify-wsdl-address'. Expected [BOOLEAN] but was");
        assertContains(errorLog, "                  STRING");
    }

    /*
     * use webservices:1.1 instead of webservices:2.0 schema
     */
    @Test
    @ServerConfig(configuration = "standalone.xml", xmlTransformationGroovy = "ModifySubsystemConfiguration.groovy",
            subtreeName = "subsystem", subsystemName = "webservices",
            parameterName = "configurationXml",
            parameterValue =
                "        <subsystem xmlns=\"urn:jboss:domain:webservices:1.1\">\n"
                + "            <wsdl-host>${jboss.bind.address:127.0.0.1}</wsdl-host>\n"
                + "            <endpoint-config name=\"Standard-Endpoint-Config\"/>\n"
                + "            <endpoint-config name=\"Recording-Endpoint-Config\">\n"
                + "                <pre-handler-chain name=\"recording-handlers\" protocol-bindings=\"##SOAP11_HTTP ##SOAP11_HTTP_MTOM ##SOAP12_HTTP ##SOAP12_HTTP_MTOM\">\n"
                + "                    <handler name=\"RecordingHandler\" class=\"org.jboss.ws.common.invocation.RecordingServerHandler\"/>\n"
                + "                </pre-handler-chain>\n"
                + "            </endpoint-config>\n"
                + "            <client-config name=\"Standard-Client-Config\"/>\n"
                + "        </subsystem>"
    )
    public void oldSubsystemVersionOnNewerConfiguration()throws Exception {
        container().tryStartAndWaitForFail();

        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "OPVDX001: Validation error in standalone.xml");
        assertContains(errorLog, "^^^^ 'client-config' isn't an allowed element here");
        assertContains(errorLog, "Elements allowed here are: endpoint-config, modify-wsdl-address,");
        assertContains(errorLog, "wsdl-host, wsdl-port, wsdl-secure-port");
    }

    /*
     * duplicate wsdl-host element
     */
    @Test
    @ServerConfig()
    public void duplicateWsdlHostElement() throws Exception {
        container().tryStartAndWaitForFail(
                (OfflineCommand) ctx -> ctx.client.apply(GroovyXmlTransform.of(DoNothing.class, "AddElement.groovy")
                        .subtree("path", Subtree.subsystem("webservices")).parameter("elementXml", "<wsdl-host>127.0.0.1</wsdl-host>")
                        .build()));
        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "OPVDX001: Validation error in standalone.xml");
        assertContains(errorLog, "^^^^ 'wsdl-host' can't appear more than once within the subsystem element");
        assertContains(errorLog, "A 'wsdl-host' element first appears here:");
    }

}
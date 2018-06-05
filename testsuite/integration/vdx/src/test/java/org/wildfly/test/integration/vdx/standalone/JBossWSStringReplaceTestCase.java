package org.wildfly.test.integration.vdx.standalone;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.vdx.TestBase;
import org.wildfly.test.integration.vdx.category.StandaloneTests;
import org.wildfly.test.integration.vdx.utils.server.Server;
import org.wildfly.test.integration.vdx.utils.server.ServerConfig;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Scanner;

/**
 *
 * Created by rsvoboda on 12/14/16.
 */
@RunAsClient
@RunWith(Arquillian.class)
@Category(StandaloneTests.class)
public class JBossWSStringReplaceTestCase extends TestBase {

    private static final Path standaloneXml = Server.CONFIGURATION_PATH.resolve("standalone.xml");
    private static final Path patchedStandaloneXml = Server.CONFIGURATION_PATH.resolve("standalone-ws-broken.xml");

    private static final String brokenWSSubsystemDefinition =
              "        <subsystem xmlns=\"urn:jboss:domain:webservices:2.0\">\n"
            + "            <wsdl-host>${jboss.bind.address:127.0.0.1}</wsdl-host>\n"
            + "            <mmodify-wsdl-address>true</modify-wsdl-address>\n"
            + "            <endpoint-config name=\"Standard-Endpoint-Config\"/>\n"
            + "            <endpoint-config name=\"Recording-Endpoint-Config\">\n"
            + "                <pre-handler-chain name=\"recording-handlers\" protocol-bindings=\"##SOAP11_HTTP ##SOAP11_HTTP_MTOM ##SOAP12_HTTP ##SOAP12_HTTP_MTOM\">\n"
            + "                    <handler name=\"RecordingHandler\" class=\"org.jboss.ws.common.invocation.RecordingServerHandler\"/>\n"
            + "                </pre-handler-chain>\n"
            + "            </endpoint-config>\n"
            + "            <client-config name=\"Standard-Client-Config\"/>\n"
            + "        </subsystem>";

    @BeforeClass
    public static void prepareBrokenConfiguration() throws IOException {
        try (Scanner scanner = new Scanner(standaloneXml);
            PrintWriter printWriter = new PrintWriter(patchedStandaloneXml.toFile())) {
            boolean inWSSubsystem = false;

            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                if (line.contains("<subsystem xmlns=\"urn:jboss:domain:webservices")) {
                    inWSSubsystem = true;

                } else if (!inWSSubsystem) {
                    printWriter.println(line);

                } else if (line.contains("</subsystem>")) {
                    printWriter.println(brokenWSSubsystemDefinition);
                    inWSSubsystem = false;
                }
            }
            printWriter.flush();
            printWriter.close();
        }
    }
    /*
     * <mmodify-wsdl-address>true</modify-wsdl-address> instead of <modify-wsdl-address>true</modify-wsdl-address>
     */
    @Test
    @ServerConfig(configuration = "standalone-ws-broken.xml")
    public void incorrectValueOfModifyWsdlAddressOpeningElement()throws Exception {
        container().tryStartAndWaitForFail();

        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "OPVDX001: Validation error in standalone-ws-broken.xml");
        assertContains(errorLog, "<mmodify-wsdl-address>true</modify-wsdl-address>");
        assertContains(errorLog, "^^^^ 'mmodify-wsdl-address' isn't an allowed element here");
        assertContains(errorLog, "matching end-tag \"</mmodify-wsdl-address>");

    }
}
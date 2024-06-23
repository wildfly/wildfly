/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertEquals;

/**
 * <ṕ>
 * IIOP subsystem tests.
 * </ṕ>
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="sguilhen@jboss.com">Stefan Guilhen</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class IIOPSubsystem3_0TestCase extends AbstractSubsystemBaseTest {

    public IIOPSubsystem3_0TestCase() {
        super(IIOPExtension.SUBSYSTEM_NAME, new IIOPExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem-3.0.xml");
    }


    @Test
    public void testExpressions() throws Exception {
        standardSubsystemTest("expressions.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-iiop-openjdk_3_0.xsd";
    }

    @Test
    public void testParseEmptySubsystem() throws Exception {
        // parse the subsystem xml into operations.
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                        "</subsystem>";
        List<ModelNode> operations = super.parse(subsystemXml);

        // check that we have the expected number of operations.
        Assert.assertEquals(1, operations.size());

        // check that each operation has the correct content.
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ADD, addSubsystem.get(OP).asString());
        PathAddress addr = PathAddress.pathAddress(addSubsystem.get(OP_ADDR));
        Assert.assertEquals(1, addr.size());
        PathElement element = addr.getElement(0);
        Assert.assertEquals(SUBSYSTEM, element.getKey());
        Assert.assertEquals(IIOPExtension.SUBSYSTEM_NAME, element.getValue());
    }

    @Test
    public void testParseSubsystemWithBadChild() throws Exception {
        // try parsing a XML with an invalid element.
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                        "   <invalid/>" +
                        "</subsystem>";
        try {
            super.parse(subsystemXml);
            Assert.fail("Should not have parsed bad child");
        } catch (XMLStreamException expected) {
        }

        // now try parsing a valid element in an invalid position.
        subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                        "    <orb>" +
                        "        <poa/>" +
                        "    </orb>" +
                        "</subsystem>";
        try {
            super.parse(subsystemXml);
            Assert.fail("Should not have parsed bad child");
        } catch (XMLStreamException expected) {
        }

    }

    @Test
    public void testSubsystemWithSecurityIdentity() throws Exception {
        super.standardSubsystemTest("subsystem-security-identity.xml");
    }

    @Test
    public void testSubsystemWithSecurityClient() throws Exception {
        super.standardSubsystemTest("subsystem-security-client.xml");
    }

    @Test
    public void testSubsystem_1_0() throws Exception {
        super.standardSubsystemTest("subsystem-1.0.xml", false);
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        //
    }

    /**
     * Verifies that attributes with expression are handled properly.
     * @throws Exception for any test failures
     */
    @Test
    public void testExpressionInAttributeValue() throws Exception {

        final String subsystemXml = readResource("subsystem-full-expressions.xml");
        final KernelServices ks = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(subsystemXml).build();
        final ModelNode iiop = ks.readWholeModel().get("subsystem", getMainSubsystemName());

        final String giopVersion = iiop.get(Constants.ORB_GIOP_VERSION).resolve().asString();
        assertEquals("1.2", giopVersion);

        final String highWaterMark = iiop.get(Constants.TCP_HIGH_WATER_MARK).resolve().asString();
        assertEquals("500", highWaterMark);

        final String numberToReclaim = iiop.get(Constants.TCP_NUMBER_TO_RECLAIM).resolve().asString();
        assertEquals("30", numberToReclaim);

        final String security = iiop.get(Constants.SECURITY).resolve().asString();
        assertEquals("elytron", security);

        final String authenticationContext = iiop.get(Constants.ORB_INIT_AUTH_CONTEXT).resolve().asString();
        assertEquals("iiop", authenticationContext);

        final String transactions = iiop.get(Constants.ORB_INIT_TRANSACTIONS).resolve().asString();
        assertEquals("spec", transactions);

        final String rootContext = iiop.get(Constants.NAMING_ROOT_CONTEXT).resolve().asString();
        assertEquals("JBoss/Naming/root2", rootContext);

        final String exportCorbaloc = iiop.get(Constants.NAMING_EXPORT_CORBALOC).resolve().asString();
        assertEquals("false", exportCorbaloc);

        final String supportSsl = iiop.get(Constants.SECURITY_SUPPORT_SSL).resolve().asString();
        assertEquals("true", supportSsl);

        final String addComponentViaInterceptor = iiop.get(Constants.SECURITY_ADD_COMP_VIA_INTERCEPTOR).resolve().asString();
        assertEquals("false", addComponentViaInterceptor);

        final String clientRequiresSsl = iiop.get(Constants.SECURITY_CLIENT_REQUIRES_SSL).resolve().asString();
        assertEquals("false", clientRequiresSsl);

        final String serverRequiresSsl = iiop.get(Constants.SECURITY_SERVER_REQUIRES_SSL).resolve().asString();
        assertEquals("false", serverRequiresSsl);

        final String interopIona = iiop.get(Constants.INTEROP_IONA).resolve().asString();
        assertEquals("false", interopIona);

        final String integrity = iiop.get(Constants.IOR_TRANSPORT_INTEGRITY).resolve().asString();
        assertEquals("required", integrity);

        final String confidentiality = iiop.get(Constants.IOR_TRANSPORT_CONFIDENTIALITY).resolve().asString();
        assertEquals("required", confidentiality);

        final String trustInClient = iiop.get(Constants.IOR_TRANSPORT_TRUST_IN_CLIENT).resolve().asString();
        assertEquals("required", trustInClient);

        final String trustInTarget = iiop.get(Constants.IOR_TRANSPORT_TRUST_IN_TARGET).resolve().asString();
        assertEquals("supported", trustInTarget);

        final String detectReply = iiop.get(Constants.IOR_TRANSPORT_DETECT_REPLAY).resolve().asString();
        assertEquals("supported", detectReply);

        final String detectMisordering = iiop.get(Constants.IOR_TRANSPORT_DETECT_MISORDERING).resolve().asString();
        assertEquals("supported", detectMisordering);

        final String authMethod = iiop.get(Constants.IOR_AS_CONTEXT_AUTH_METHOD).resolve().asString();
        assertEquals("none", authMethod);

        final String realm = iiop.get(Constants.IOR_AS_CONTEXT_REALM).resolve().asString();
        assertEquals("test_realm2", realm);

        final String required = iiop.get(Constants.IOR_AS_CONTEXT_REQUIRED).resolve().asString();
        assertEquals("true", required);

        final String callerPropagation = iiop.get(Constants.IOR_SAS_CONTEXT_CALLER_PROPAGATION).resolve().asString();
        assertEquals("supported", callerPropagation);

    }

}

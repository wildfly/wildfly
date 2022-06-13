package org.wildfly.extension.undertow;

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SecurityDomainSingleSignOnAttributesTestCase extends AbstractSubsystemTest {

    public SecurityDomainSingleSignOnAttributesTestCase() {
        super(UndertowExtension.SUBSYSTEM_NAME, new UndertowExtension());
    }

    @Test
    public void testPathChangeRequiresReload() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(AbstractUndertowSubsystemTestCase.RUNTIME).setSubsystemXml(readResource("undertow-13.0.xml"));
        KernelServices mainServices = builder.build();
        PathAddress address = PathAddress.pathAddress("subsystem", "undertow")
                .append("application-security-domain", "other")
                .append("setting", "single-sign-on");
        ModelNode result = mainServices.executeOperation(
                Operations.createWriteAttributeOperation(address, SingleSignOnDefinition.Attribute.PATH,
                        new ModelNode("/modified-path")));
        assertEquals("success", result.get("outcome").asString());
        assertTrue("It is expected that reload is required after the operation.",
                result.get("response-headers").get("operation-requires-reload").asBoolean());
    }

}

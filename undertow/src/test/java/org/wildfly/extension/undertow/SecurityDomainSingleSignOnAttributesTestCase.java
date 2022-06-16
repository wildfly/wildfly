package org.wildfly.extension.undertow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

public class SecurityDomainSingleSignOnAttributesTestCase extends AbstractSubsystemTest {

    public SecurityDomainSingleSignOnAttributesTestCase() {
        super(UndertowExtension.SUBSYSTEM_NAME, new UndertowExtension());
    }

    @Test
    public void testPathChangeRequiresReload() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(AbstractUndertowSubsystemTestCase.RUNTIME).setSubsystemXml(readResource("undertow-13.0.xml"));
        KernelServices mainServices = builder.build();
        PathAddress address = PathAddress.pathAddress(UndertowExtension.SUBSYSTEM_PATH, PathElement.pathElement(Constants.APPLICATION_SECURITY_DOMAIN, "other"), UndertowExtension.PATH_SSO);
        ModelNode result = mainServices.executeOperation(Util.getWriteAttributeOperation(address, SingleSignOnDefinition.Attribute.PATH.getName(), new ModelNode("/modified-path")));
        assertEquals(ModelDescriptionConstants.SUCCESS, result.get(ModelDescriptionConstants.OUTCOME).asString());
        assertTrue("It is expected that reload is required after the operation.", result.get(ModelDescriptionConstants.RESPONSE_HEADERS).get(ModelDescriptionConstants.OPERATION_REQUIRES_RELOAD).asBoolean());
    }

}

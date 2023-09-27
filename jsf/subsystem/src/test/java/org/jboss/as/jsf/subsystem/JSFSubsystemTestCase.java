/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jsf.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *
 * @author Stuart Douglas
 */
public class JSFSubsystemTestCase extends AbstractSubsystemBaseTest {
    public JSFSubsystemTestCase() {
        super(JSFExtension.SUBSYSTEM_NAME, new JSFExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return "<subsystem xmlns=\"urn:jboss:domain:jsf:1.1\"" +
                " default-jsf-impl-slot=\"${exp.default-jsf-impl-slot:main}\"" +
                " disallow-doctype-decl=\"${exp.disallow-doctype-decl:true}\" />";
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-jsf_1_1.xsd";
    }

    @Test
    public void testAttributes() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(getSubsystemXml());
        KernelServices kernelServices = builder.build();
        ModelNode rootModel = kernelServices.readWholeModel();
        ModelNode serverModel = rootModel.require(SUBSYSTEM).require(JSFExtension.SUBSYSTEM_NAME);
        assertEquals("main", serverModel.get(JSFResourceDefinition.DEFAULT_SLOT_ATTR_NAME).resolve().asString());
        assertEquals(true, serverModel.get(JSFResourceDefinition.DISALLOW_DOCTYPE_DECL_ATTR_NAME).resolve().asBoolean());
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.subsystems.complextestcases;

import java.util.Properties;

import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public class ComplexResourceAdaptersSubsystemTestCase extends AbstractComplexSubsystemTestCase {

    public ComplexResourceAdaptersSubsystemTestCase() {
        super(ResourceAdaptersExtension.SUBSYSTEM_NAME, new ResourceAdaptersExtension());
    }

    @Test
    public void testResourceAdapters() throws Exception {

        ModelNode model = getModel("ra.xml", "some.rar");
        if (model == null)
            return;
        // Check model..
        Properties params = ParseUtils.raCommonProperties();
        ModelNode raCommonModel = model.get("subsystem", "resource-adapters", "resource-adapter", "myRA");
        ParseUtils.checkModelParams(raCommonModel, params);
        Assert.assertEquals(raCommonModel.asString(), "A", raCommonModel.get("config-properties", "Property", "value")
                .asString());
        Assert.assertEquals(raCommonModel.get("beanvalidationgroups").asString(), "[\"Class0\",\"Class00\"]",
                raCommonModel.get("beanvalidationgroups").asString());

        params = ParseUtils.raAdminProperties();
        ModelNode raAdminModel = raCommonModel.get("admin-objects", "Pool2");
        ParseUtils.checkModelParams(raAdminModel, params);
        Assert.assertEquals(raAdminModel.asString(), "D", raAdminModel.get("config-properties", "Property", "value").asString());

        params = ParseUtils.raConnectionProperties();
        ModelNode raConnModel = raCommonModel.get("connection-definitions", "Pool1");
        ParseUtils.checkModelParams(raConnModel, params);
        Assert.assertEquals(raConnModel.asString(), "B", raConnModel.get("config-properties", "Property", "value").asString());
        Assert.assertEquals(raConnModel.asString(), "C", raConnModel.get("recovery-plugin-properties", "Property").asString());
    }

    @Test
    public void testResourceAdapterWith2ConDefAnd2AdmObj() throws Exception {

        getModel("ra2.xml", false, "multiple.rar");

    }
}

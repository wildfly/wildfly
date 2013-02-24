/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
        Assert.assertEquals(raCommonModel.get("beanvalidationgroups").asString(), raCommonModel.get("beanvalidationgroups")
                .asString(), "[\"Class0\",\"Class00\"]");

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

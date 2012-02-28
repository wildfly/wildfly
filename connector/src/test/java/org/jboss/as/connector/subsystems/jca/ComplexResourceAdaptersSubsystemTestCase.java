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
package org.jboss.as.connector.subsystems.jca;

import static org.jboss.as.connector.subsystems.jca.ParseUtils.checkModelParams;
import static org.jboss.as.connector.subsystems.jca.ParseUtils.raAdminProperties;
import static org.jboss.as.connector.subsystems.jca.ParseUtils.raCommonProperties;
import static org.jboss.as.connector.subsystems.jca.ParseUtils.raConnectionProperties;

import java.util.Properties;

import junit.framework.Assert;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Ignore;
import org.junit.Test;

/**
 * 
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public class ComplexResourceAdaptersSubsystemTestCase extends AbstractSubsystemTest {

    public ComplexResourceAdaptersSubsystemTestCase() {
        super(ResourceAdaptersExtension.SUBSYSTEM_NAME, new ResourceAdaptersExtension());
    }

    public ModelNode getModel(String resourceFileName) throws Exception {
        return getModel(resourceFileName, true);
    }

    public ModelNode getModel(String resourceFileName, boolean checkMarshalledXML) throws Exception {

        String xml = readResource(resourceFileName);

        KernelServices services = super.installInController(AdditionalInitialization.MANAGEMENT, xml);

        ModelNode model = services.readWholeModel();
        ConnectorServices.unregisterResourceIdentifiers("some.rar");

        // Marshal the xml to see that it is the same as before
        String marshalled = services.getPersistedSubsystemXml();
        if (checkMarshalledXML)
            Assert.assertEquals(normalizeXML(xml), normalizeXML(marshalled));

        services = super.installInController(AdditionalInitialization.MANAGEMENT, marshalled);

        // Check that the model looks the same
        ModelNode modelReloaded = services.readWholeModel();
        compare(model, modelReloaded);

        assertRemoveSubsystemResources(services);
        return model;

    }

    @Test
    public void testResourceAdapters() throws Exception {

        ModelNode model = getModel("ra.xml");
        if (model == null)
            return;
        // Check model..
        Properties params = raCommonProperties();
        ModelNode raCommonModel = model.get("subsystem", "resource-adapters", "resource-adapter", "some.rar");
        checkModelParams(raCommonModel, params);
        Assert.assertEquals(raCommonModel.asString(), "A", raCommonModel.get("config-properties", "Property", "value")
                .asString());
        Assert.assertEquals(raCommonModel.get("beanvalidationgroups").asString(), raCommonModel.get("beanvalidationgroups")
                .asString(), "[\"Class0\",\"Class00\"]");

        params = raAdminProperties();
        ModelNode raAdminModel = raCommonModel.get("admin-objects", "Pool2");
        checkModelParams(raAdminModel, params);
        Assert.assertEquals(raAdminModel.asString(), "D", raAdminModel.get("config-properties", "Property", "value").asString());

        params = raConnectionProperties();
        ModelNode raConnModel = raCommonModel.get("connection-definitions", "Pool1");
        checkModelParams(raConnModel, params);
        Assert.assertEquals(raConnModel.asString(), "B", raConnModel.get("config-properties", "Property", "value").asString());
        Assert.assertEquals(raConnModel.asString(), "C", raConnModel.get("recovery-plugin-properties", "Property").asString());
    }

    @Test
    @Ignore("AS7-3941")
    public void testResourceAdapterWith2ConDefAnd2AdmObj() throws Exception {

        getModel("ra2.xml", false);

    }
}

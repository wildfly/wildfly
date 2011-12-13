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

import static org.jboss.as.connector.subsystems.jca.ParseUtils.commonDsProperties;
import static org.jboss.as.connector.subsystems.jca.ParseUtils.raCommonProperties;
import static org.jboss.as.connector.subsystems.jca.ParseUtils.raConnectionProperties;
import static org.jboss.as.connector.subsystems.jca.ParseUtils.raAdminProperties;

import static org.jboss.as.connector.subsystems.jca.ParseUtils.controlModelParams;

import java.util.List;
import java.util.Properties;

import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension;
import javax.xml.stream.XMLStreamException;

import junit.framework.Assert;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.junit.Ignore;
import org.jboss.as.controller.OperationContext.Type;

/**
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public class ComplexResourceAdaptersSubsystemTestCase extends AbstractSubsystemTest {


    public ComplexResourceAdaptersSubsystemTestCase() {
        super(ResourceAdaptersExtension.SUBSYSTEM_NAME, new ResourceAdaptersExtension());
    }

    @Test
    @Ignore("AS7-3007")

    public void testResourceAdapters() throws Exception{
        
        String xml = readResource("ra.xml");
 
        KernelServices services = super.installInController(new AdditionalInitialization() {
 
            @Override
            protected Type getType() {
                //This override makes it only install in the model, not create the services
                return Type.MANAGEMENT;
            }
 
        }, xml);
 
        ModelNode model = services.readWholeModel();
 
        // Check model..
        Properties params=raCommonProperties();
        ModelNode raCommonModel=model.get("subsystem", "resource-adapters","resource-adapter","some.rar");
        controlModelParams(raCommonModel,params);
        Assert.assertEquals(raCommonModel.asString(),"A",raCommonModel.get("config-properties","Property","value").asString());

        params=raAdminProperties();
        ModelNode raAdminModel=raCommonModel.get("admin-objects", "Pool2");
        controlModelParams(raAdminModel,params);
        Assert.assertEquals(raAdminModel.asString(),"D",raAdminModel.get("config-properties","Property","value").asString());

        params=raConnectionProperties();
        ModelNode raConnModel=raCommonModel.get("connection-definitions", "Pool1");
        controlModelParams(raConnModel,params);
        Assert.assertEquals(raConnModel.asString(),"B",raConnModel.get("config-properties","Property","value").asString());
        Assert.assertEquals(raConnModel.asString(),"C",raConnModel.get("recover-plugin-config-properties","Property","value").asString());

        //Marshal the xml to see that it is the same as before
        String marshalled = services.getPersistedSubsystemXml();
        Assert.assertEquals(normalizeXML(xml), normalizeXML(marshalled));
 
        services = super.installInController(new AdditionalInitialization() {
 
            @Override
            protected Type getType() {
                //This override makes it only install in the model, not create the services
                return Type.MANAGEMENT;
            }
 
        }, marshalled);
 
        //Check that the model looks the same
        ModelNode modelReloaded = services.readWholeModel();
        compare(model, modelReloaded);
    }
}

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
package org.jboss.as.naming.subsystem;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class NamingSubsystemTestCase extends AbstractSubsystemBaseTest {

    public NamingSubsystemTestCase() {
        super(NamingExtension.SUBSYSTEM_NAME, new NamingExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws IOException {
        return "schema/jboss-as-naming_2_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
                "/subsystem-templates/naming.xml"
        };
    }

    @Test
    public void testOnlyExternalContextAllowsCache() throws Exception {
        KernelServices services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .build();
        Assert.assertTrue(services.isSuccessfulBoot());

        List<ModelNode> list = parse(ModelTestUtils.readResource(this.getClass(), "subsystem.xml"));

        for (ModelNode addOp : list) {
            PathAddress addr = PathAddress.pathAddress(addOp.require(ModelDescriptionConstants.OP_ADDR));
            if (addr.size() == 2 && addr.getLastElement().getKey().equals(NamingSubsystemModel.BINDING) && BindingType.forName(addOp.get(NamingBindingResourceDefinition.BINDING_TYPE.getName()).asString()) != BindingType.EXTERNAL_CONTEXT) {
                //Add the cache attribute and make sure it fails
                addOp.get(NamingBindingResourceDefinition.CACHE.getName()).set(true);
                services.executeForFailure(addOp);

                //Remove the cache attribute and make sure it succeeds
                addOp.remove(NamingBindingResourceDefinition.CACHE.getName());
                ModelTestUtils.checkOutcome(services.executeOperation(addOp));

                //Try to write the cache attribute, which should fail
                ModelTestUtils.checkFailed(services.executeOperation(Util.getWriteAttributeOperation(addr, NamingBindingResourceDefinition.CACHE.getName(), new ModelNode(true))));

            } else {
                ModelTestUtils.checkOutcome(services.executeOperation(addOp));
            }
        }

        for (int i = 1 ; i < list.size() ; i++) {

        }
    }
}

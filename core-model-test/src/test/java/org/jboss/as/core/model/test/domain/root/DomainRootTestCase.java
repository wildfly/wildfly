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
package org.jboss.as.core.model.test.domain.root;

import junit.framework.Assert;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.server.controller.descriptions.ServerDescriptionConstants;
import org.jboss.as.version.Version;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DomainRootTestCase extends AbstractCoreModelTest {

    @Test
    public void testEmptyRoot() throws Exception {
        KernelServices kernelServices = createEmptyRoot();

        ModelNode model = kernelServices.readWholeModel(false, true);
        assertAttribute(model, ModelDescriptionConstants.NAMESPACES, new ModelNode().setEmptyList());
        assertAttribute(model, ModelDescriptionConstants.SCHEMA_LOCATIONS, new ModelNode().setEmptyList());
        assertAttribute(model, ModelDescriptionConstants.NAME, new ModelNode("Unnamed Domain"));
        assertAttribute(model, ModelDescriptionConstants.PRODUCT_NAME, null);
        assertAttribute(model, ModelDescriptionConstants.PRODUCT_VERSION, null);
        assertAttribute(model, ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION, new ModelNode(Version.MANAGEMENT_MAJOR_VERSION));
        assertAttribute(model, ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION, new ModelNode(Version.MANAGEMENT_MINOR_VERSION));
        assertAttribute(model, ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION, new ModelNode(Version.MANAGEMENT_MICRO_VERSION));
        assertAttribute(model, ServerDescriptionConstants.PROCESS_TYPE, new ModelNode("Domain Controller"));
        assertAttribute(model, ServerDescriptionConstants.LAUNCH_TYPE, new ModelNode("DOMAIN"));

        //These two cannot work in tests - placeholder
        assertAttribute(model, ModelDescriptionConstants.RELEASE_VERSION, new ModelNode("Unknown"));
        assertAttribute(model, ModelDescriptionConstants.RELEASE_CODENAME, new ModelNode("Unknown"));

        //Try changing namespaces, schema-locations and name
    }

    @Test
    public void testWriteName() throws Exception {
        KernelServices kernelServices = createEmptyRoot();

        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
        op.get(ModelDescriptionConstants.OP_ADDR).setEmptyList();
        op.get(ModelDescriptionConstants.NAME).set(ModelDescriptionConstants.NAME);
        op.get(ModelDescriptionConstants.VALUE).set("");
        kernelServices.executeForFailure(op);

        op.get(ModelDescriptionConstants.VALUE).set("Testing");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));

        ModelNode model = kernelServices.readWholeModel(false, true);
        assertAttribute(model, ModelDescriptionConstants.NAME, new ModelNode("Testing"));

        op = new ModelNode();
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION);
        op.get(ModelDescriptionConstants.OP_ADDR).setEmptyList();
        op.get(ModelDescriptionConstants.NAME).set(ModelDescriptionConstants.NAME);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));

        model = kernelServices.readWholeModel(false, true);
        assertAttribute(model, ModelDescriptionConstants.NAME, new ModelNode("Unnamed Domain"));
    }

    private void assertAttribute(ModelNode model, String name, ModelNode expected) {
        if (expected == null) {
            Assert.assertFalse(model.get(name).isDefined());
        } else {
            ModelNode actual = model.get(name);
            Assert.assertEquals(expected, actual);
        }
    }

    private KernelServices createEmptyRoot() throws Exception{
        KernelServices kernelServices = createKernelServicesBuilder(TestModelType.DOMAIN)
                .build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());
        return kernelServices;
    }
}

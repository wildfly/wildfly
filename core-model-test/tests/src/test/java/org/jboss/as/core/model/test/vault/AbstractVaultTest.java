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
package org.jboss.as.core.model.test.vault;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CODE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_OPTIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AbstractVaultTest extends AbstractCoreModelTest {
    private final PathAddress vaultAddress;

    protected AbstractVaultTest(PathAddress vaultAddress) {
        this.vaultAddress = vaultAddress;
    }

    @Test
    public void testAddAndRemoveEmptyVault() throws Exception {
        KernelServices kernelServices = createEmptyRoot();

        ModelNode model = readVaultModel(kernelServices);
        Assert.assertFalse(model.isDefined());

        ModelNode add = Util.createAddOperation(vaultAddress);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(add));
        model = readVaultModel(kernelServices);
        Assert.assertTrue(model.isDefined());

        Assert.assertFalse(model.get(CODE).isDefined());
        Assert.assertFalse(model.get(VAULT_OPTIONS).isDefined());

        ModelNode remove = Util.createRemoveOperation(vaultAddress);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(remove));
        model = readVaultModel(kernelServices);
        Assert.assertFalse(model.isDefined());
    }

    @Test
    public void testAddAndRemoveVaultWithData() throws Exception {
        KernelServices kernelServices = createEmptyRoot();


        ModelNode add = Util.createAddOperation(vaultAddress);
        add.get(CODE).set("test");
        add.get(VAULT_OPTIONS, "one").set("111");
        add.get(VAULT_OPTIONS, "two").set("222");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(add));
        ModelNode model = readVaultModel(kernelServices);
        Assert.assertTrue(model.isDefined());

        Assert.assertEquals("test", model.get(CODE).asString());
        Assert.assertTrue(model.get(VAULT_OPTIONS).isDefined());
        Assert.assertEquals(2, model.get(VAULT_OPTIONS).keys().size());
        Assert.assertEquals("111", model.get(VAULT_OPTIONS, "one").asString());
        Assert.assertEquals("222", model.get(VAULT_OPTIONS, "two").asString());

        ModelNode remove = Util.createRemoveOperation(vaultAddress);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(remove));
    }

    @Test
    public void testAddAndRemoveVaultWithBadOptions() throws Exception {
        KernelServices kernelServices = createEmptyRoot();

        ModelNode add = Util.createAddOperation(vaultAddress);
        add.get(CODE).set("test");
        add.get(VAULT_OPTIONS).set(1);
        kernelServices.executeForFailure(add);

        add = Util.createAddOperation(vaultAddress);
        add.get(CODE).set("test");
        add.get(VAULT_OPTIONS, "one").setEmptyList();
        kernelServices.executeForFailure(add);
    }

    @Test
    public void testWriteVaultAttributes() throws Exception {
        KernelServices kernelServices = createEmptyRoot();
        ModelNode add = Util.createAddOperation(vaultAddress);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(add));

        ModelNode writeCode = Util.createOperation(WRITE_ATTRIBUTE_OPERATION, vaultAddress);
        writeCode.get(NAME).set(CODE);
        writeCode.get(VALUE).add("bad");
        kernelServices.executeForFailure(writeCode);

        ModelNode writeOptions = Util.createOperation(WRITE_ATTRIBUTE_OPERATION, vaultAddress);
        writeOptions.get(NAME).set(VAULT_OPTIONS);
        writeOptions.get(VALUE).set("bad");
        kernelServices.executeForFailure(writeOptions);

        writeOptions = Util.createOperation(WRITE_ATTRIBUTE_OPERATION, vaultAddress);
        writeOptions.get(NAME).set(VAULT_OPTIONS);
        writeOptions.get(VALUE).add("one");
        kernelServices.executeForFailure(writeOptions);

        writeOptions = Util.createOperation(WRITE_ATTRIBUTE_OPERATION, vaultAddress);
        writeOptions.get(NAME).set(VAULT_OPTIONS);
        writeOptions.get(VALUE, "one").set("111");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(writeOptions));

        writeCode = Util.createOperation(WRITE_ATTRIBUTE_OPERATION, vaultAddress);
        writeCode.get(NAME).set(CODE);
        writeCode.get(VALUE).set("thecode");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(writeCode));

        ModelNode model = readVaultModel(kernelServices);
        Assert.assertEquals("thecode", model.get(CODE).asString());
        Assert.assertEquals(1, model.get(VAULT_OPTIONS).keys().size());
        Assert.assertEquals("111", model.get(VAULT_OPTIONS, "one").asString());
    }

    @Test
    public void testVaultXml() throws Exception {
        KernelServices kernelServices = createKernelServicesBuilder()
                .setXmlResource(getXmlResource())
                .build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());

        ModelNode model = readVaultModel(kernelServices);
        Assert.assertEquals("somevault", model.get(CODE).asString());
        Assert.assertEquals(2, model.get(VAULT_OPTIONS).keys().size());
        Assert.assertEquals("zxc", model.get(VAULT_OPTIONS, "xyz").asString());
        Assert.assertEquals("def", model.get(VAULT_OPTIONS, "abc").asString());

        ModelTestUtils.compareXml(ModelTestUtils.readResource(this.getClass(), getXmlResource()), kernelServices.getPersistedSubsystemXml());
    }

    protected ModelNode readVaultModel(KernelServices kernelServices) throws Exception {
        return ModelTestUtils.getSubModel(kernelServices.readWholeModel(), vaultAddress);
    }

    protected abstract KernelServices createEmptyRoot() throws Exception;

    protected abstract KernelServicesBuilder createKernelServicesBuilder();

    protected abstract String getXmlResource();

}

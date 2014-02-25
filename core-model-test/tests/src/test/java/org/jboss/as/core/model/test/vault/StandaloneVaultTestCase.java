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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_OPTIONS;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class StandaloneVaultTestCase extends AbstractVaultTest {

    public StandaloneVaultTestCase() {
        super(PathAddress.EMPTY_ADDRESS);
    }

    @Test
    public void testValidVaultExpressionInSysProperty() throws Exception {
        KernelServices kernelServices = createEmptyRoot();

        ModelNode composite = Util.createEmptyOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
        ModelNode steps = composite.get(STEPS).setEmptyList();

        ModelNode propOneAdd = Util.createAddOperation(rootAddress.append(PathElement.pathElement(SYSTEM_PROPERTY, "vault-prop-one")));
        propOneAdd.get(VALUE).set("${VAULT::test::one::xxx}");
        steps.add(propOneAdd);

        ModelNode propTwoAdd = Util.createAddOperation(rootAddress.append(PathElement.pathElement(SYSTEM_PROPERTY, "vault-prop-two")));
        propTwoAdd.get(VALUE).set("${VAULT::test::two::xxx}");
        steps.add(propTwoAdd);

        ModelNode vaultAdd = Util.createAddOperation(vaultAddress);
        vaultAdd.get(CODE).set("test");
        vaultAdd.get(VAULT_OPTIONS, "one").set("111");
        vaultAdd.get(VAULT_OPTIONS, "two").set("222");
        steps.add(vaultAdd);

        System.clearProperty("vault-prop-one");
        System.clearProperty("vault-prop-two");

        ModelTestUtils.checkOutcome(kernelServices.executeOperation(composite));
        ModelNode model = readVaultModel(kernelServices);
        Assert.assertTrue(model.isDefined());

        Assert.assertEquals("111", System.getProperty("vault-prop-one"));
        Assert.assertEquals("222", System.getProperty("vault-prop-two"));
    }

    @Test
    public void testInvalidVaultExpressionInSysProperty() throws Exception {
        KernelServices kernelServices = createEmptyRoot();

        ModelNode composite = Util.createEmptyOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
        ModelNode steps = composite.get(STEPS).setEmptyList();

        // A system property with a valid vault expression
        ModelNode propOneAdd = Util.createAddOperation(rootAddress.append(PathElement.pathElement(SYSTEM_PROPERTY, "vault-prop-one")));
        propOneAdd.get(VALUE).set("${VAULT::test::one::xxx}");
        steps.add(propOneAdd);

        // A system property with an invalid vault expression
        ModelNode propTwoAdd = Util.createAddOperation(rootAddress.append(PathElement.pathElement(SYSTEM_PROPERTY, "vault-prop-two")));
        propTwoAdd.get(VALUE).set("${VAULT::WRONG::two::xxx}");  // incorrect vault name; should not resolve
        steps.add(propTwoAdd);

        ModelNode vaultAdd = Util.createAddOperation(vaultAddress);
        vaultAdd.get(CODE).set("test");
        vaultAdd.get(VAULT_OPTIONS, "one").set("111");
        vaultAdd.get(VAULT_OPTIONS, "two").set("222");
        steps.add(vaultAdd);

        System.clearProperty("vault-prop-one");
        System.clearProperty("vault-prop-two");

        ModelTestUtils.checkFailed(kernelServices.executeOperation(composite));
        ModelNode model = readVaultModel(kernelServices);
        Assert.assertFalse(model.isDefined());

        Assert.assertNull("vault-prop-one is not null", System.getProperty("vault-prop-one"));
        Assert.assertNull("vault-prop-two is not null", System.getProperty("vault-prop-two"));
    }

    protected KernelServices createEmptyRoot() throws Exception {
        KernelServices kernelServices = createKernelServicesBuilder().build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());
        return kernelServices;
    }

    protected KernelServicesBuilder createKernelServicesBuilder() {
        return super.createKernelServicesBuilder(TestModelType.STANDALONE);
    }

    @Override
    protected String getXmlResource() {
        return "vault-standalone.xml";
    }
}

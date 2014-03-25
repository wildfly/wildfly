/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.vault;

import java.io.File;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.server.controller.resources.VaultResourceDefinition;
import org.jboss.as.test.manualmode.vault.module.CustomSecurityVault;
import org.jboss.as.test.manualmode.vault.module.TestVaultExtension;
import org.jboss.as.test.manualmode.vault.module.TestVaultParser;
import org.jboss.as.test.manualmode.vault.module.TestVaultRemoveHandler;
import org.jboss.as.test.manualmode.vault.module.TestVaultResolveExpressionHandler;
import org.jboss.as.test.manualmode.vault.module.TestVaultSubsystemResourceDescription;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ValueExpression;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;


/**
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CustomVaultInModuleTestCase {

	private static final String CONTAINER = "default-jbossas";

	private static final String MODULE_NAME = "test.custom.vault.in.module";

    @ArquillianResource
    private static ContainerController containerController;

    private TestModule testModule;

    @Test
    public void testCustomVault() throws Exception {
    	ModelControllerClient client = null;
        containerController.start(CONTAINER);

    	try {
    		client = TestSuiteEnvironment.getModelControllerClient();

        	ModelNode op = createResolveExpressionOp("${VAULT::Testing::Stuff::thing}");
        	ModelNode result = client.execute(op);
        	Assert.assertEquals("123_Testing_Stuff_thing", ModelTestUtils.checkResultAndGetContents(result).asString());

        	op = createResolveExpressionOp("${VAULT::Another::Something::whatever}");
            result = client.execute(op);
            Assert.assertEquals("Hello_Another_Something_whatever", ModelTestUtils.checkResultAndGetContents(result).asString());

            op = createResolveExpressionOp("${VAULT::Nothing::is::here}");
            ModelTestUtils.checkFailed(client.execute(op));

    	} finally {
    		IoUtils.safeClose(client);
    	}
    }

    @Before
    public void setupServer() throws Exception {
    	createTestModule();
    	setupServerWithVault();
    }

    @After
    public void tearDownServer() throws Exception {
    	ModelControllerClient client = null;
    	try {
    		client = TestSuiteEnvironment.getModelControllerClient();
    		try {
    		    ModelNode vaultResult = client.execute(Util.createRemoveOperation(PathAddress.pathAddress(VaultResourceDefinition.PATH)));
                ModelNode subsystemResult = client.execute(Util.createRemoveOperation(PathAddress.pathAddress(TestVaultSubsystemResourceDescription.PATH)));
                ModelNode extensionResult = client.execute(Util.createRemoveOperation(PathAddress.pathAddress(ModelDescriptionConstants.EXTENSION, MODULE_NAME)));
                ModelTestUtils.checkOutcome(vaultResult);
                ModelTestUtils.checkOutcome(subsystemResult);
                ModelTestUtils.checkOutcome(extensionResult);
    		} finally {
    		    containerController.stop(CONTAINER);
    		}
    	} finally {
    		testModule.remove();
    		IoUtils.safeClose(client);
    	}
    }

    private void createTestModule() throws Exception {
        File moduleXml = new File(CustomSecurityVault.class.getResource(CustomVaultInModuleTestCase.class.getSimpleName() + "-module.xml").toURI());
        testModule = new TestModule(MODULE_NAME, moduleXml);

        JavaArchive archive = testModule.addResource("test-custom-vault-in-module.jar")
        	.addClass(CustomSecurityVault.class)
            .addClass(TestVaultExtension.class)
            .addClass(TestVaultParser.class)
        	.addClass(TestVaultRemoveHandler.class)
        	.addClass(TestVaultResolveExpressionHandler.class)
        	.addClass(TestVaultSubsystemResourceDescription.class);

        ArchivePath path = ArchivePaths.create("/");
        path = ArchivePaths.create(path, "services");
        path = ArchivePaths.create(path, Extension.class.getName());
        archive.addAsManifestResource(CustomSecurityVault.class.getPackage(), Extension.class.getName(), path);

        testModule.create(true);
    }

    private void setupServerWithVault() throws Exception {
    	ModelControllerClient client = null;
    	ManagementClient managementClient = null;
    	containerController.start(CONTAINER);
    	try {
	        client = TestSuiteEnvironment.getModelControllerClient();
	        managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
	                TestSuiteEnvironment.getServerPort(), "http-remoting");

	        //Add the vault
	        final ModelNode addVault = Util.createAddOperation(PathAddress.pathAddress(VaultResourceDefinition.PATH));
	        addVault.get(ModelDescriptionConstants.MODULE).set(MODULE_NAME);
	        addVault.get(ModelDescriptionConstants.CODE).set(CustomSecurityVault.class.getName());
	        final ModelNode options = new ModelNode();
	        options.get("Testing").set("123");
	        options.get("Another").set("Hello");
	        addVault.get(ModelDescriptionConstants.VAULT_OPTIONS).set(options);
	        ModelTestUtils.checkOutcome(client.execute(addVault));

	        //Add the extension
	        final ModelNode addExtension = Util.createAddOperation(PathAddress.pathAddress(ModelDescriptionConstants.EXTENSION, MODULE_NAME));
	        ModelTestUtils.checkOutcome(client.execute(addExtension));

	        final ModelNode addSubsystem = Util.createAddOperation(PathAddress.pathAddress(TestVaultSubsystemResourceDescription.PATH));
	        ModelTestUtils.checkOutcome(client.execute(addSubsystem));

    	} finally {
    		containerController.stop(CONTAINER);
    		long end = System.currentTimeMillis() + TimeoutUtil.adjust(3000);
    		do {
    		    Thread.sleep(50);
    		} while (managementClient.isServerInRunningState() && System.currentTimeMillis() < end);

    		IoUtils.safeClose(client);
    		IoUtils.safeClose(managementClient);
    	}
    }

    private ModelNode createResolveExpressionOp(String expression){
        ModelNode op = Util.createOperation(TestVaultResolveExpressionHandler.RESOLVE.getName(), PathAddress.pathAddress(TestVaultSubsystemResourceDescription.PATH));
        op.get(TestVaultResolveExpressionHandler.PARAM_EXPRESSION.getName()).set(new ValueExpression(expression));
        return op;
    }
}

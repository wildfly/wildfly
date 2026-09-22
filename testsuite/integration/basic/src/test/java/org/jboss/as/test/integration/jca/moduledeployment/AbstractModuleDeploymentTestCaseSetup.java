/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.moduledeployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROCESS_STATE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.connector.subsystems.resourceadapters.Namespace;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdapterSubsystemParser;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;

/**
 * AS7-5768 -Support for RA module deployment
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 * @author <a href="istudens@redhat.com">Ivo Studensky</a>
 */
public abstract class AbstractModuleDeploymentTestCaseSetup extends AbstractMgmtServerSetupTask {

    private final List<TestModule> testModuleList = new ArrayList<>();
    protected File testModuleRoot;
    protected File slot;
    public static ModelNode address;
    protected final String defaultPath = "org/jboss/ironjacamar/ra16out";
    private boolean reloadRequired = false;

    public void addModule(final String moduleName) throws Exception {
        addModule(moduleName, "module.xml");
    }

    public void addModule(final String moduleName, String moduleXml) throws Exception {
        testModuleRoot = new File(TestModule.getModulesDirectory(true), moduleName);
        createTestModule(moduleName, moduleXml);
    }

    public void removeModule() throws Exception {
        for (TestModule testModule : testModuleList) {
            testModule.remove();
        }
    }

    private void createTestModule(String moduleName, String moduleXml) throws IOException, URISyntaxException {
        URL url = this.getClass().getResource(moduleXml);
        if (url == null) {
            throw new IllegalStateException("Could not find " + moduleXml);
        }
        File moduleXmlFile = new File(url.toURI());
        TestModule testModule = new TestModule(moduleName.replace('/', '.'), moduleXmlFile);
        testModule.create();
        testModuleList.add(testModule);
        this.slot = new File(testModuleRoot, "main");
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        try {
            remove(address, managementClient);
        } finally {
            removeModule();
        }
        if (reloadRequired) {
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }
    }

    protected void remove(final ModelNode address, final ManagementClient managementClient) throws IOException, MgmtOperationException {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("remove");
        operation.get(OP_ADDR).set(address);
        final ModelNode result = managementClient.getControllerClient().execute(operation);

        if (!SUCCESS.equals(result.get(OUTCOME).asString())) {
            throw new MgmtOperationException("Module removal failed: " + result.get(FAILURE_DESCRIPTION), operation, result);
        }
        final ModelNode responseHeaders = result.get(RESPONSE_HEADERS);
        if (responseHeaders.hasDefined(PROCESS_STATE) && ControlledProcessState.State.RELOAD_REQUIRED.toString().equals(responseHeaders.get(PROCESS_STATE).asString())) {
            this.reloadRequired = true;
        }
    }

    @Override
    protected void doSetup(ManagementClient managementClient) throws Exception {
        addModule(defaultPath);
    }

    protected void setConfiguration(String fileName) throws Exception {
        String xml = FileUtils.readFile(this.getClass(), fileName);
        List<ModelNode> operations = xmlToModelOperations(xml,
                Namespace.CURRENT.getUriString(),
                new ResourceAdapterSubsystemParser());
        address = operations.get(1).get("address");
        operations.remove(0);
        for (ModelNode op : operations) {
            executeOperation(op);
        }
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("activate");
        operation.get(OP_ADDR).set(address);
        executeOperation(operation);
    }

    /**
     * Creates module structure for uncompressed RA archive. RA classes are in
     * flat form too
     *
     * @throws Exception
     */
    protected void fillModuleWithFlatClasses(String raFile) throws Exception {
        ResourceAdapterArchive rar = ShrinkWrap
                .create(ResourceAdapterArchive.class);
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ra16out.jar");
        jar.addPackage(MultipleConnectionFactory1.class.getPackage());
        rar.addAsManifestResource(this.getClass().getPackage(), raFile,
                "ra.xml");
        rar.as(ExplodedExporter.class).exportExploded(testModuleRoot, "main");
        jar.as(ExplodedExporter.class).exportExploded(testModuleRoot, "main");
    }

    /**
     * Creates module structure for uncompressed RA archive.
     * RA classes are packed in .jar archive
     *
     * @throws Exception
     */
    protected void fillModuleWithJar(String raFile) throws Exception {
        ResourceAdapterArchive rar = ShrinkWrap
                .create(ResourceAdapterArchive.class);
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ra16out.jar");
        jar.addPackage(MultipleConnectionFactory1.class.getPackage());
        rar.addAsManifestResource(
                PureJarTestCase.class.getPackage(), raFile, "ra.xml");
        rar.as(ExplodedExporter.class).exportExploded(testModuleRoot, "main");

        Files.copy(jar.as(ZipExporter.class).exportAsInputStream(), new File(slot, "ra16out.jar").toPath());
    }

    /**
     * Returns basic address
     *
     * @return address
     */
    public static ModelNode getAddress() {
        return address;
    }

}

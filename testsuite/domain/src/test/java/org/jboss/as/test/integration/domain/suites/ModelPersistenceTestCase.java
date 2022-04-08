/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.domain.suites;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.integration.management.util.SimpleServlet;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests both automated and manual configuration model persistence snapshot generation.
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public class ModelPersistenceTestCase {

    enum Host { MASTER, SLAVE }

    private class CfgFileDescription {

        CfgFileDescription(int version, File file, long hash) {
            this.version = version;
            this.file = file;
            this.hash = hash;
        }
        public int version;
        public File file;
        public long hash;
    }
    private static DomainTestSupport domainSupport;
    private static DomainLifecycleUtil domainMasterLifecycleUtil;
    private static DomainLifecycleUtil domainSlaveLifecycleUtil;
    private static final String DOMAIN_HISTORY_DIR = "domain_xml_history";
    private static final String HOST_HISTORY_DIR = "host_xml_history";
    private static final String CONFIG_DIR = "configuration";
    private static final String CURRENT_DIR = "current";
    private static final String MASTER_DIR = "master";
    private static final String SLAVE_DIR = "slave";
    private static final String DOMAIN_NAME = "testing-domain-standard";
    private static final String MASTER_NAME = "testing-host-master";
    private static final String SLAVE_NAME = "testing-host-slave";
    private static File domainCurrentCfgDir;
    private static File masterCurrentCfgDir;
    private static File slaveCurrentCfgDir;
    private static File domainLastCfgFile;
    private static File masterLastCfgFile;
    private static File slaveLastCfgFile;

    @BeforeClass
    public static void initDomain() throws Exception {
        domainSupport = DomainTestSuite.createSupport(ModelPersistenceTestCase.class.getSimpleName());
        domainMasterLifecycleUtil = domainSupport.getDomainMasterLifecycleUtil();
        domainSlaveLifecycleUtil = domainSupport.getDomainSlaveLifecycleUtil();

        File masterDir = new File(domainSupport.getDomainMasterConfiguration().getDomainDirectory());
        File slaveDir = new File(domainSupport.getDomainSlaveConfiguration().getDomainDirectory());
        domainCurrentCfgDir = new File(masterDir, CONFIG_DIR
                + File.separator + DOMAIN_HISTORY_DIR + File.separator + CURRENT_DIR);
        masterCurrentCfgDir = new File(masterDir,  CONFIG_DIR
                + File.separator + HOST_HISTORY_DIR + File.separator + CURRENT_DIR);
        slaveCurrentCfgDir = new File(slaveDir, CONFIG_DIR
                + File.separator + HOST_HISTORY_DIR + File.separator + CURRENT_DIR);
        domainLastCfgFile = new File(masterDir, CONFIG_DIR + File.separator
                + DOMAIN_HISTORY_DIR + File.separator + DOMAIN_NAME + ".last.xml");
        masterLastCfgFile = new File(masterDir, CONFIG_DIR + File.separator
                + HOST_HISTORY_DIR + File.separator + MASTER_NAME + ".last.xml");
        slaveLastCfgFile = new File(slaveDir, CONFIG_DIR + File.separator
                + HOST_HISTORY_DIR + File.separator + SLAVE_NAME + ".last.xml");
    }

    @AfterClass
    public static void shutdownDomain() {
        domainSupport = null;
        domainMasterLifecycleUtil = null;
        domainSlaveLifecycleUtil = null;
        DomainTestSuite.stopSupport();
    }

    @Test
    public void testSimpleDomainOperation() throws Exception {
        ModelNode op = ModelUtil.createOpNode("profile=default/subsystem=ee", WRITE_ATTRIBUTE_OPERATION);
        op.get(NAME).set("ear-subdeployments-isolated");
        op.get(VALUE).set(true);
        testDomainOperation(op);
        op.get(VALUE).set(false);
        testDomainOperation(op);
    }

    @Test
    public void testCompositeDomainOperation() throws Exception {
        ModelNode[] steps = new ModelNode[2];
        steps[0] = ModelUtil.createOpNode("profile=default/subsystem=ee", WRITE_ATTRIBUTE_OPERATION);
        steps[0].get(NAME).set("ear-subdeployments-isolated");
        steps[0].get(VALUE).set(true);

        steps[1] = ModelUtil.createOpNode("system-property=model-persistence-test", ADD);
        steps[1].get(VALUE).set("test");

        testDomainOperation(ModelUtil.createCompositeNode(steps));
        steps[0].get(VALUE).set(false);
        steps[1] = ModelUtil.createOpNode("system-property=model-persistence-test", REMOVE);
        testDomainOperation(ModelUtil.createCompositeNode(steps));
    }

    private void testDomainOperation(ModelNode operation) throws Exception {

        DomainClient client = domainMasterLifecycleUtil.getDomainClient();
        CfgFileDescription lastBackupDesc = getLatestBackup(domainCurrentCfgDir);
        CfgFileDescription lastMasterBackupDesc = getLatestBackup(masterCurrentCfgDir);
        CfgFileDescription lastSlaveBackupDesc = getLatestBackup(slaveCurrentCfgDir);
        long lastFileHash = domainLastCfgFile.exists() ? FileUtils.checksumCRC32(domainLastCfgFile) : -1;

        // execute operation so the model gets updated
        executeOperation(client, operation);

        // check that the automated snapshot of the domain has been generated
        CfgFileDescription newBackupDesc = getLatestBackup(domainCurrentCfgDir);
        Assert.assertNotNull("Model snapshot not found.", newBackupDesc);
        // check that the version is incremented by one
        Assert.assertTrue(lastBackupDesc.version == newBackupDesc.version - 1);

        // check that the both master and slave host snapshot have not been generated
        CfgFileDescription newMasterBackupDesc = getLatestBackup(masterCurrentCfgDir);
        CfgFileDescription newSlaveBackupDesc = getLatestBackup(slaveCurrentCfgDir);
        Assert.assertTrue(lastMasterBackupDesc.version == newMasterBackupDesc.version);
        Assert.assertTrue(lastSlaveBackupDesc.version == newSlaveBackupDesc.version);

        // check that the last cfg file has changed
        Assert.assertTrue(lastFileHash != FileUtils.checksumCRC32(domainLastCfgFile));
    }

    @Test
    public void testDomainOperationRollback() throws Exception {

        DomainClient client = domainMasterLifecycleUtil.getDomainClient();

        CfgFileDescription lastDomainBackupDesc = getLatestBackup(domainCurrentCfgDir);
        CfgFileDescription lastMasterBackupDesc = getLatestBackup(masterCurrentCfgDir);
        CfgFileDescription lastSlaveBackupDesc = getLatestBackup(slaveCurrentCfgDir);

        // execute operation so the model gets updated
        ModelNode op = ModelUtil.createOpNode("system-property=model-persistence-test", "add");
        op.get(VALUE).set("test");
        executeAndRollbackOperation(client, op);

        // check that the model has not been updated
        CfgFileDescription newDomainBackupDesc = getLatestBackup(domainCurrentCfgDir);
        CfgFileDescription newMasterBackupDesc = getLatestBackup(masterCurrentCfgDir);
        CfgFileDescription newSlaveBackupDesc = getLatestBackup(slaveCurrentCfgDir);

        // check that the configs did not change
        Assert.assertTrue(lastDomainBackupDesc.version == newDomainBackupDesc.version);
        Assert.assertTrue(lastMasterBackupDesc.version == newMasterBackupDesc.version);
        Assert.assertTrue(lastSlaveBackupDesc.version == newSlaveBackupDesc.version);
    }

    @Test
    public void testSimpleHostOperation() throws Exception {

        // using master DC
        ModelNode op = ModelUtil.createOpNode("host=master/system-property=model-persistence-test", ADD);
        op.get(VALUE).set("test");
        testHostOperation(op, Host.MASTER, Host.MASTER);
        op = ModelUtil.createOpNode("host=master/system-property=model-persistence-test", REMOVE);
        testHostOperation(op, Host.MASTER, Host.MASTER);

        op = ModelUtil.createOpNode("host=slave/system-property=model-persistence-test", ADD);
        op.get(VALUE).set("test");
        testHostOperation(op, Host.MASTER, Host.SLAVE);
        op = ModelUtil.createOpNode("host=slave/system-property=model-persistence-test", REMOVE);
        testHostOperation(op, Host.MASTER, Host.SLAVE);

        // using slave HC
        op = ModelUtil.createOpNode("host=slave/system-property=model-persistence-test", ADD);
        op.get(VALUE).set("test");
        testHostOperation(op, Host.SLAVE, Host.SLAVE);
        op = ModelUtil.createOpNode("host=slave/system-property=model-persistence-test", REMOVE);
        testHostOperation(op, Host.SLAVE, Host.SLAVE);
    }

    @Test
    public void testCompositeHostOperation() throws Exception {

        // test op on master using master controller
        ModelNode[] steps = new ModelNode[2];
        steps[0] = ModelUtil.createOpNode("host=master/system-property=model-persistence-test", ADD);
        steps[0].get(VALUE).set("test");
        steps[1] = ModelUtil.createOpNode("host=master/system-property=model-persistence-test", "write-attribute");
        steps[1].get(NAME).set("value");
        steps[1].get(VALUE).set("test2");
        testHostOperation(ModelUtil.createCompositeNode(steps),Host.MASTER, Host.MASTER);

        ModelNode op = ModelUtil.createOpNode("host=master/system-property=model-persistence-test", REMOVE);
        testHostOperation(op,Host.MASTER,  Host.MASTER);

        // test op on slave using master controller
        steps[0] = ModelUtil.createOpNode("host=slave/system-property=model-persistence-test", ADD);
        steps[0].get(VALUE).set("test");
        steps[1] = ModelUtil.createOpNode("host=slave/system-property=model-persistence-test", "write-attribute");
        steps[1].get(NAME).set("value");
        steps[1].get(VALUE).set("test2");
        testHostOperation(ModelUtil.createCompositeNode(steps),Host.MASTER,  Host.SLAVE);

        op = ModelUtil.createOpNode("host=slave/system-property=model-persistence-test", REMOVE);
        testHostOperation(op,Host.MASTER,  Host.SLAVE);

        // test op on slave using slave controller
        steps[0] = ModelUtil.createOpNode("host=slave/system-property=model-persistence-test", ADD);
        steps[0].get(VALUE).set("test");
        steps[1] = ModelUtil.createOpNode("host=slave/system-property=model-persistence-test", "write-attribute");
        steps[1].get(NAME).set("value");
        steps[1].get(VALUE).set("test2");
        testHostOperation(ModelUtil.createCompositeNode(steps), Host.SLAVE,  Host.SLAVE);

        op = ModelUtil.createOpNode("host=slave/system-property=model-persistence-test", REMOVE);
        testHostOperation(op, Host.SLAVE,  Host.SLAVE);

    }

    @Test
    public void testHostOperationRollback() throws Exception {

        DomainClient client = domainMasterLifecycleUtil.getDomainClient();

        for (Host host : Host.values()) {

            CfgFileDescription lastDomainBackupDesc = getLatestBackup(domainCurrentCfgDir);
            CfgFileDescription lastMasterBackupDesc = getLatestBackup(masterCurrentCfgDir);
            CfgFileDescription lastSlaveBackupDesc = getLatestBackup(slaveCurrentCfgDir);

            // execute operation so the model gets updated
            ModelNode op = host.equals(Host.MASTER) ?
                    ModelUtil.createOpNode("host=master/system-property=model-persistence-test", "add") :
                    ModelUtil.createOpNode("host=slave/system-property=model-persistence-test", "add") ;
            op.get(VALUE).set("test");
            executeAndRollbackOperation(client, op);

            // check that the model has not been updated
            CfgFileDescription newDomainBackupDesc = getLatestBackup(domainCurrentCfgDir);
            CfgFileDescription newMasterBackupDesc = getLatestBackup(masterCurrentCfgDir);
            CfgFileDescription newSlaveBackupDesc = getLatestBackup(slaveCurrentCfgDir);

            // check that the configs did not change
            Assert.assertTrue(lastDomainBackupDesc.version == newDomainBackupDesc.version);
            Assert.assertTrue(lastMasterBackupDesc.version == newMasterBackupDesc.version);
            Assert.assertTrue(lastSlaveBackupDesc.version == newSlaveBackupDesc.version);

        }
    }

    private void testHostOperation(ModelNode operation, Host controller, Host target) throws Exception {

        DomainClient client = controller.equals(Host.MASTER) ?
                domainMasterLifecycleUtil.getDomainClient() : domainSlaveLifecycleUtil.getDomainClient();

        CfgFileDescription lastDomainBackupDesc = getLatestBackup(domainCurrentCfgDir);
        CfgFileDescription lastMasterBackupDesc = getLatestBackup(masterCurrentCfgDir);
        CfgFileDescription lastSlaveBackupDesc = getLatestBackup(slaveCurrentCfgDir);
        long lastDomainFileHash = domainLastCfgFile.exists() ? FileUtils.checksumCRC32(domainLastCfgFile) : -1;
        long lastMasterFileHash = masterLastCfgFile.exists() ? FileUtils.checksumCRC32(masterLastCfgFile) : -1;
        long lastSlaveFileHash = slaveLastCfgFile.exists() ? FileUtils.checksumCRC32(slaveLastCfgFile) : -1;

        // execute operation so the model gets updated
        executeOperation(client, operation);

        // check that the automated snapshot of the domain has not been generated
        CfgFileDescription newDomainBackupDesc = getLatestBackup(domainCurrentCfgDir);
        Assert.assertTrue(lastDomainBackupDesc.version == newDomainBackupDesc.version);

        // check that only the appropriate host snapshot has been generated
        CfgFileDescription newMasterBackupDesc = getLatestBackup(masterCurrentCfgDir);
        CfgFileDescription newSlaveBackupDesc = getLatestBackup(slaveCurrentCfgDir);
        if (target == Host.MASTER) {
            Assert.assertTrue(lastMasterBackupDesc.version == newMasterBackupDesc.version - 1);
            Assert.assertTrue(lastSlaveBackupDesc.version == newSlaveBackupDesc.version);
            Assert.assertTrue(lastMasterFileHash != FileUtils.checksumCRC32(masterLastCfgFile));
            Assert.assertTrue(lastSlaveFileHash == FileUtils.checksumCRC32(slaveLastCfgFile));
        } else {
            Assert.assertTrue(lastMasterBackupDesc.version == newMasterBackupDesc.version);
            Assert.assertTrue(lastSlaveBackupDesc.version == newSlaveBackupDesc.version - 1);
            Assert.assertTrue(lastMasterFileHash == FileUtils.checksumCRC32(masterLastCfgFile));
            Assert.assertTrue(lastSlaveFileHash != FileUtils.checksumCRC32(slaveLastCfgFile));
        }
        Assert.assertTrue(lastDomainBackupDesc.version == newDomainBackupDesc.version);
        Assert.assertTrue(lastDomainFileHash == FileUtils.checksumCRC32(domainLastCfgFile));

    }

    @Test
    public void testTakeAndDeleteSnapshot() throws Exception {

        DomainClient client = domainMasterLifecycleUtil.getDomainClient();

        // take snapshot
        ModelNode op = ModelUtil.createOpNode(null, "take-snapshot");
        ModelNode result = executeOperation(client, op);

        // check that the snapshot file exists
        String snapshotFileName = result.asString();
        File snapshotFile = new File(snapshotFileName);
        Assert.assertTrue(snapshotFile.exists());

        // compare with current cfg
        long snapshotHash = FileUtils.checksumCRC32(snapshotFile);
        long lastHash = FileUtils.checksumCRC32(domainLastCfgFile);
        Assert.assertTrue(snapshotHash == lastHash);

        // delete snapshot
        op = ModelUtil.createOpNode(null, "delete-snapshot");
        op.get("name").set(snapshotFile.getName());
        executeOperation(client, op);

        // check that the file is deleted
        Assert.assertFalse("Snapshot file still exists.", snapshotFile.exists());


    }

    private CfgFileDescription getLatestBackup(File dir) throws IOException {

        int lastVersion = 0;
        File lastFile = null;

        File[] children;
        if (dir.isDirectory() && (children = dir.listFiles()) != null) {
            for (File file : children) {

                String fileName = file.getName();
                String[] nameParts = fileName.split("\\.");
                if (! (nameParts[0].contains(DOMAIN_NAME) || nameParts[0].contains(MASTER_NAME) || nameParts[0].contains(SLAVE_NAME) )) {
                    continue;
                }
                if (!nameParts[2].equals("xml")) {
                    continue;
                }
                int version = Integer.valueOf(nameParts[1].substring(1));
                if (version > lastVersion) {
                    lastVersion = version;
                    lastFile = file;
                }
            }
        }
        return new CfgFileDescription(lastVersion, lastFile, (lastFile != null) ? FileUtils.checksumCRC32(lastFile) : 0);
    }

    protected ModelNode executeOperation(DomainClient client, final ModelNode op) throws IOException, MgmtOperationException {
        return executeOperation(client, op, true);
    }

    protected ModelNode executeOperation(DomainClient client, final ModelNode op, boolean unwrapResult) throws IOException, MgmtOperationException {
        ModelNode ret = client.execute(op);
        if (!unwrapResult) {
            return ret;
        }

        if (!SUCCESS.equals(ret.get(OUTCOME).asString())) {
            throw new MgmtOperationException("Management operation failed: " + ret.get(FAILURE_DESCRIPTION), op, ret);
        }
        return ret.get(RESULT);
    }

    protected void executeAndRollbackOperation(DomainClient client, final ModelNode op) throws IOException, OperationFormatException {

        ModelNode addDeploymentOp = ModelUtil.createOpNode("deployment=malformedDeployment.war", "add");
        addDeploymentOp.get("content").get(0).get("input-stream-index").set(0);

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
        builder.setOperationName("deploy");
        builder.addNode("deployment", "malformedDeployment.war");


        ModelNode[] steps = new ModelNode[3];
        steps[0] = op;
        steps[1] = addDeploymentOp;
        steps[2] = builder.buildRequest();
        ModelNode compositeOp = ModelUtil.createCompositeNode(steps);

        OperationBuilder ob = new OperationBuilder(compositeOp, true);
        ob.addInputStream(new FileInputStream(getBrokenWar()));

        ModelNode ret =  client.execute(ob.build());
        Assert.assertFalse(SUCCESS.equals(ret.get(OUTCOME).asString()));
    }

    private static File getBrokenWar() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "malformedDeployment.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebInfResource(new StringAsset("Malformed"), "web.xml");
        File brokenWar = new File(System.getProperty("java.io.tmpdir") + File.separator + "malformedDeployment.war");
        brokenWar.deleteOnExit();
        new ZipExporterImpl(war).exportTo(brokenWar, true);
        return brokenWar;
    }
}

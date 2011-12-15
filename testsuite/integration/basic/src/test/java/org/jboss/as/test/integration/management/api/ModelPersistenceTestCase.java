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
package org.jboss.as.test.integration.management.api;

import java.util.List;
import org.jboss.dmr.ModelNode;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.cli.GlobalOpsTestCase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

/**
 * Tests both automated and manual configuration model persistence snapshot generation.
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ModelPersistenceTestCase extends AbstractMgmtTestBase {
    
    private class CfgFileDescription {
        
        public CfgFileDescription(int version, File file, long hash) {
            this.version = version;
            this.file = file;
            this.hash = hash;
        }
        
        public int version;
        public File file;
        public long hash;        
        
    }
    
    private static final String SERVER_CONFIG_DIR = System.getProperty("jboss.inst") + "/standalone/configuration";
    private static final String HISTORY_DIR = "standalone_xml_history";
    private static final String CURRENT_DIR = "current";
            
    private static File configDir;
    private static File currentCfgDir;
    private static File lastCfgFile;
    
    @ArquillianResource
    URL url;
    
    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(GlobalOpsTestCase.class);
        return ja;
    }
        
    @Before
    public void before() throws IOException, MgmtOperationException {
        
        initModelControllerClient(url.getHost(), MGMT_PORT);
        
        if (configDir == null) {        
            configDir = new File(SERVER_CONFIG_DIR);
            assertTrue("Server config dir " + SERVER_CONFIG_DIR + " does not exists.", configDir.exists());
            assertTrue(configDir.isDirectory());
            currentCfgDir = new File(configDir, HISTORY_DIR + File.separatorChar + CURRENT_DIR);
            
            // get server configuration name
            ModelNode op = createOpNode("core-service=server-environment", "read-attribute"); 
            op.get("name").set("config-file");
            ModelNode result = executeOperation(op);          
            String configFile = result.asString();
            String configFileName = new File(configFile).getName();
            assertTrue(configFileName.endsWith(".xml"));
            configFileName = configFileName.substring(0, configFileName.length() - 4);
            
            lastCfgFile = new File(configDir, HISTORY_DIR + File.separator + configFileName + ".last.xml");
        }
        
    }
    
    @AfterClass
    public static void after() throws IOException {
        closeModelControllerClient();
    }    
    
    @Test
    public void testSimpleOperation() throws Exception {                
        
        CfgFileDescription lastBackupDesc = getLatestBackup(currentCfgDir);
        
        long lastFileHash = lastCfgFile.exists() ?  FileUtils.checksumCRC32(lastCfgFile) : -1;
        
        // execute operation so the model gets updated
        ModelNode op = createOpNode("system-property=test", "add"); 
        op.get("value").set("test");
        executeOperation(op);
                
        // check that the automated snapshat has been generated
        CfgFileDescription newBackupDesc = getLatestBackup(currentCfgDir);
        assertNotNull("Model snapshot not found.", newBackupDesc);
        // check that the version is incremented by one
        assertTrue(lastBackupDesc.version == newBackupDesc.version -1);
        
        // check that the last cfg file has changed
        assertTrue(lastFileHash != FileUtils.checksumCRC32(lastCfgFile));
        
        // remove testing attribute
        op = createOpNode("system-property=test", "remove"); 
        executeOperation(op);        
        
        // check that the snapshot has been updated again
        lastBackupDesc = newBackupDesc;
        newBackupDesc = getLatestBackup(currentCfgDir);
        assertNotNull("Model snapshot not found.", newBackupDesc);
        // check that the version is incremented by one
        assertTrue(lastBackupDesc.version == newBackupDesc.version -1);        
    }

    @Test
    public void testCompositeOperation() throws Exception {                
                
        CfgFileDescription lastBackupDesc = getLatestBackup(currentCfgDir);

        // execute composite operation
        ModelNode[] steps = new ModelNode[2];
        steps[0] = createOpNode("system-property=test", "add"); 
        steps[0].get("value").set("test");
        steps[1] = createOpNode("system-property=test", "write-attribute"); 
        steps[1].get("name").set("value");
        steps[1].get("value").set("test2");       
        executeOperation(ModelUtil.createCompositeNode(steps));
        
        // check that the automated snapshat has been generated
        CfgFileDescription newBackupDesc = getLatestBackup(currentCfgDir);
        // check that the version is incremented by one
        assertTrue(lastBackupDesc.version == newBackupDesc.version -1);
        
        
        // remove testing attribute
        ModelNode op = createOpNode("system-property=test", "remove"); 
        executeOperation(op);          
     
        // check that the snapshot has been updated again
        lastBackupDesc = newBackupDesc;
        
        newBackupDesc = getLatestBackup(currentCfgDir);
        assertNotNull("Model snapshot not found.", newBackupDesc);
        // check that the version is incremented by one
        assertTrue(lastBackupDesc.version == newBackupDesc.version -1);           
    }

    @Test
    public void testCompositeOperationRollback() throws Exception {                
                
        CfgFileDescription lastBackupDesc = getLatestBackup(currentCfgDir);
        
        // execute operation so the model gets updated
        ModelNode op = createOpNode("system-property=test", "add"); 
        op.get("value").set("test");
        executeAndRollbackOperation(op);
        
        // check that the model has not been updated
        CfgFileDescription newBackupDesc = getLatestBackup(currentCfgDir);
        assertNotNull("Model snapshot not found.", newBackupDesc);
        
        // check that the config did not change
        assertTrue(lastBackupDesc.version == newBackupDesc.version);
        assertTrue(lastBackupDesc.hash == newBackupDesc.hash);
                         
    }
    
    
    @Test
    public void testTakeAndDeleteSnapshot() throws Exception {
        
        // take snapshot

        ModelNode op = createOpNode(null, "take-snapshot"); 
        ModelNode result = executeOperation(op);
        
        // check that the snapshot file exists
        String snapshotFileName = result.asString();        
        File snapshotFile = new File(snapshotFileName);
        assertTrue(snapshotFile.exists());
        
        // compare with current cfg        
        long snapshotHash = FileUtils.checksumCRC32(snapshotFile);
        long lastHash = FileUtils.checksumCRC32(lastCfgFile);
        assertTrue(snapshotHash == lastHash);
        
        // delete snapshot
        op = createOpNode(null, "delete-snapshot"); 
        op.get("name").set(snapshotFile.getName());
        result = executeOperation(op);
        
        // check that the file is deleted
        assertFalse("Snapshot file stil exists.", snapshotFile.exists());
        
    }

    @Test
    public void testListSnapshots() throws Exception {
        
        // take snapshot
        ModelNode op = createOpNode(null, "take-snapshot"); 
        ModelNode result = executeOperation(op);
        
        // check that the snapshot file exists
        String snapshotFileName = result.asString();        
        File snapshotFile = new File(snapshotFileName);
        assertTrue(snapshotFile.exists());
        
        // get the snapshot listing
        op = createOpNode(null, "list-snapshots"); 
        result = executeOperation(op);
        File snapshotDir = new File(result.get("directory").asString());
        assertTrue(snapshotDir.isDirectory());        
        
        List<String> snapshotNames = ModelUtil.modelNodeAsStingList(result.get("names"));
        assertTrue(snapshotNames.contains(snapshotFile.getName()));
        
    }    
    private CfgFileDescription getLatestBackup(File dir) throws IOException {
        
        int lastVersion = 0;        
        File lastFile = null;        
        
        if (dir.isDirectory())
            for (File file : dir.listFiles()) {

                String fileName = file.getName();
                String[] nameParts = fileName.split("\\.");
                if (! nameParts[0].contains("standalone")) continue;
                if (! nameParts[2].equals("xml")) continue;
                int version = Integer.valueOf(nameParts[1].substring(1));
                if (version > lastVersion) {
                    lastVersion = version;
                    lastFile = file;
                }
            }
        return new CfgFileDescription(lastVersion, lastFile, (lastFile != null) ? FileUtils.checksumCRC32(lastFile) : 0);
    }
}

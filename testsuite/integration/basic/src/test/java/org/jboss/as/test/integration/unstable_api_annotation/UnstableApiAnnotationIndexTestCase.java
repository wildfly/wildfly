/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.unstable_api_annotation;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.as.test.integration.unstable_api_annotation.dummy.Dummy;
import org.jboss.as.test.integration.unstable_api_annotation.jar.JarClassA;
import org.jboss.as.test.integration.unstable_api_annotation.jar2.Jar2ClassA;
import org.jboss.as.test.integration.unstable_api_annotation.war.Servlet;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ClassAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;

/**
 * The full test for testing the reporting of usage of unstable API annotation annotated constructs lives
 * in WildFly Core. This test is just to verify that the annotation index from the feature packs is present in the server.
 *
 * Also, there is some verification that in various types of archives, the classes are only scanned once.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({UnstableApiAnnotationIndexTestCase.SystemPropertyServerSetupTask.class})
public class UnstableApiAnnotationIndexTestCase {

    private static final String INDEX_MODULE_DIR =
            "system/layers/base/org/wildfly/_internal/unstable-api-annotation-index/main";

    private static final String CONTENT = "content";
    private static final String INDEX_INDEX_FILE = "index.txt";

    private static final String WILDFLY_EE_FEATURE_PACK_INDEX = "wildfly-ee-feature-pack.txt";

    @ArquillianResource
    public ManagementClient managementClient;

    @Deployment(testable = false)
    public static Archive<?> getDummyDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "dummy.jar")
                .addClass(Dummy.class);
    }

    @Test
    public void testIndexExists() throws Exception {
        String[] modulePathStrings = System.getProperty("module.path", null).split(File.pathSeparator);
        List<Path> modulePaths = new ArrayList<>();
        for (String mp : modulePathStrings) {
            Path p = Paths.get(mp);
            if (Files.exists(p)) {
                modulePaths.add(p);
            }
        }

        Assert.assertTrue(modulePaths.size() > 0);

        boolean found = false;
        for (Path modulesPath : modulePaths) {
            Path indexModulePath = modulesPath.resolve(INDEX_MODULE_DIR);
            if (Files.exists(indexModulePath)) {
                found = true;

                Path indexContentDir = indexModulePath.resolve(CONTENT);
                Assert.assertTrue(Files.exists(indexContentDir));

                Path mainIndexFile = indexContentDir.resolve(INDEX_INDEX_FILE);
                Assert.assertTrue(Files.exists(mainIndexFile));

                Set<String> indices = Files.list(indexContentDir)
                        .filter(p -> !p.getFileName().toString().equals(INDEX_INDEX_FILE))
                        .map(p -> p.getFileName().toString())
                        .collect(Collectors.toSet());
                // TODO  In some cases we might be provisioning the galleon-feature-pack,
                // in others only the ee-feature-pack?
                //Assert.assertEquals(paths.toString(), 1, paths.size());

                Assert.assertTrue(indices.toString(), indices.contains(WILDFLY_EE_FEATURE_PACK_INDEX));

                List<String> mainIndexEntries = Files.readAllLines(mainIndexFile).stream().filter(l -> !l.isEmpty() && !l.startsWith("#")).collect(Collectors.toList());
                Assert.assertEquals(mainIndexEntries + " : " + indices, mainIndexEntries.size(), indices.size());
                Assert.assertTrue(mainIndexEntries + " : " + indices, indices.containsAll(mainIndexEntries));

                break;
            }
        }

        Assert.assertTrue("Could not find annotation index module", found);
    }

    @Test
    public void testJavaArchive() throws Exception {
        JavaArchive jar = createJavaArchive();
        testArchiveDeployment(jar, 2);
    }

    @Test
    public void testJavaArchive2() throws Exception {
        // Temporary check
        JavaArchive jar = createJavaArchive();
        testArchiveDeployment(jar, 2);
    }

    @Test
    public void testEmptyWebArchive() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "test-unstable-api-annotation-empty.war")
                        .add(new StringAsset("test"), "test.txt");

        testArchiveDeployment(war, 0);
    }

    @Test
    public void testWebArchiveWithClassesInWrongLocation() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "test-unstable-api-annotation-empty.war")
                        .add(new ClassAsset(Servlet.class), "org/jboss/as/test/integration/unstable_api_annotation/war/WarClassA.class");

        testArchiveDeployment(war, 0);
    }

    @Test
    public void testWebArchiveWithWebInfClasses() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "test-unstable-api-annotation-empty.war")
                        .addPackage(Servlet.class.getPackage());
        testArchiveDeployment(war, 4);
    }

    @Test
    public void testWebArchiveWithLibJar() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "test-unstable-api-annotation-empty.war")
                        .addAsLibraries(createJavaArchive());

        testArchiveDeployment(war, 2);
    }


    @Test
    public void testWebArchiveWithWebInfClassesAndLibJar() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "test-unstable-api-annotation-empty.war")
                .addPackage(Servlet.class.getPackage())
                .addAsLibraries(createJavaArchive());

        testArchiveDeployment(war, 6);
    }

    @Test
    public void testWebArchiveWithWebInfClassesAndTwoLibJar() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "test-unstable-api-annotation-empty.war")
                .addPackage(Servlet.class.getPackage())
                .addAsLibraries(createJavaArchive(), createJavaArchive2());

        testArchiveDeployment(war, 14);
    }

    @Test
    public void testEarWithOneModuleJar() throws Exception {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test-unstable-api-annotation-empty.ear");
        ear.addAsModule(createJavaArchive());
        testArchiveDeployment(ear, 2);
    }

    @Test
    public void testEarWithOneModuleJarAndClassInWrongPlace() throws Exception {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test-unstable-api-annotation-empty.ear");
        ear.addAsModule(createJavaArchive());
        ear.add(new ClassAsset(Dummy.class), "org/jboss/as/test/integration/unstable_api_annotation/dummy/Dummy.class");

        testArchiveDeployment(ear, 2);
    }

    @Test
    public void testEarWithNoModuleJarAndEmptyWar() throws Exception {

        WebArchive emptyWar = ShrinkWrap.create(WebArchive.class, "test-unstable-api-annotation-empty.war")
                .add(new ClassAsset(Servlet.class), "org/jboss/as/test/integration/unstable_api_annotation/war/WarClassA.class");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test-unstable-api-annotation-empty.ear");
        ear.addAsModule(emptyWar);

        testArchiveDeployment(ear, 0);
    }

    @Test
    public void testEarWithModuleJarAndWarWithClasses() throws Exception {

        WebArchive war = ShrinkWrap.create(WebArchive.class, "test-unstable-api-annotation-empty.war")
                .addPackage(Servlet.class.getPackage());

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test-unstable-api-annotation-empty.ear");
        ear.addAsModule(createJavaArchive());
        ear.addAsModule(war);

        testArchiveDeployment(ear, 6);
    }

    @Test
    public void testEarWithModuleJarAndWarWithClassesAndLibJar() throws Exception {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test-unstable-api-annotation-empty.ear");
        ear.addAsModule(createJavaArchive());
        ear.addAsModule(ShrinkWrap.create(WebArchive.class, "test-unstable-api-annotation-empty.war")
                .addPackage(Servlet.class.getPackage())
                .addAsLibraries(createJavaArchive2()));

        testArchiveDeployment(ear, 14);
    }

    @Test
    public void testEarWithLibJarAndWarWithClasses() throws Exception {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test-unstable-api-annotation-empty.ear");
        ear.addAsLibrary(createJavaArchive());
        ear.addAsModule(ShrinkWrap.create(WebArchive.class, "test-unstable-api-annotation-empty.war")
                .addPackage(Servlet.class.getPackage()));

        testArchiveDeployment(ear, 6);
    }

    @Test
    public void testEarWithModuleJarLibJarAndWarWithClasses() throws Exception {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test-unstable-api-annotation-empty.ear");
        ear.addAsLibrary(createJavaArchive());
        ear.addAsModule(createJavaArchive2());
        ear.addAsModule(ShrinkWrap.create(WebArchive.class, "test-unstable-api-annotation-empty.war")
                .addPackage(Servlet.class.getPackage()));

        testArchiveDeployment(ear, 14);
    }

    private void testArchiveDeployment(Archive<?> archive, int expectedClassesCount) throws Exception {
        LogDiffer logDiffer = new LogDiffer();
        logDiffer.takeSnapshot();

        ModelControllerClient mcc = managementClient.getControllerClient();

        Operation deploymentOp = createDeploymentOp(archive);
        ManagementOperations.executeOperation(mcc, deploymentOp);
        try {
            List<String> list = logDiffer.getNewLogEntries();
            checkExpectedNumberClasses(list, expectedClassesCount);
        } finally {
            ManagementOperations.executeOperation(mcc, Util.createRemoveOperation(PathAddress.pathAddress("deployment", archive.getName())));
        }
    }

    private Operation createDeploymentOp(Archive<?> deployment) {
        final List<InputStream> streams = new ArrayList<>();
        streams.add(deployment.as(ZipExporter.class).exportAsInputStream());
        final ModelNode addOperation = Util.createAddOperation(PathAddress.pathAddress("deployment", deployment.getName()));
        addOperation.get("enabled").set(true);
        addOperation.get("content").add().get("input-stream-index").set(0);
        return Operation.Factory.create(addOperation, streams, true);
    }


    private JavaArchive createJavaArchive() {
        return ShrinkWrap.create(JavaArchive.class, "test-unstable-api-annotation.jar")
                .addPackage(JarClassA.class.getPackage());
    }

    private JavaArchive createJavaArchive2() {
        return ShrinkWrap.create(JavaArchive.class, "test-unstable-api-annotation2.jar")
                .addPackage(Jar2ClassA.class.getPackage());
    }

    private void checkExpectedNumberClasses(List<String> newLogEntries, int numberClasses) {
        int classCount = 0;
        for (String logEntry : newLogEntries.stream().filter(s -> s.contains("WFLYCM0016")).collect(Collectors.toList())) {
            int index = logEntry.lastIndexOf(":");
            String count = logEntry.substring(index + 1).trim();
            classCount += Integer.parseInt(count);
        }
        Assert.assertFalse(newLogEntries.isEmpty());
        Assert.assertEquals(numberClasses, classCount);
    }

    private static class LogDiffer {
        Path logFile;

        private List<String> lastLogSnapshot = Collections.emptyList();


        public LogDiffer() {
            String jbossHomeProp = System.getProperty("jboss.home");
            Path jbossHome = Paths.get(jbossHomeProp);
            Assert.assertTrue(Files.exists(jbossHome));
            this.logFile = jbossHome.resolve("standalone/log/server.log");
            Assert.assertTrue(Files.exists(logFile));
        }

        public void takeSnapshot() {
            try {
                lastLogSnapshot = Files.readAllLines(logFile);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public List<String> getNewLogEntries() {
            try {
                List<String> currentLog = Files.readAllLines(logFile);
                return currentLog.stream()
                        .filter(s -> !lastLogSnapshot.contains(s))
                        .filter(s -> s.contains("WFLYCM"))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static class SystemPropertyServerSetupTask extends SnapshotRestoreSetupTask {
        @Override
        protected void doSetup(ManagementClient client, String containerId) throws Exception {
            super.doSetup(client, containerId);
            ModelNode op = Util.createAddOperation(PathAddress.pathAddress(SYSTEM_PROPERTY, "org.wildfly.test.unstable-api-annotation.extra-output"));
            op.get("value").set("true");
            ManagementOperations.executeOperation(client.getControllerClient(), op);
            // Reload so the system property is picked up by the deployer in order to print extra information
            // about class count
            ServerReload.executeReloadAndWaitForCompletion(client.getControllerClient());
        }
    }
}

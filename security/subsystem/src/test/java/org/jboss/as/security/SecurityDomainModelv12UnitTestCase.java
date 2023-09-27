/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.security;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.List;
import java.util.Properties;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * <p>
 * Security subsystem tests for the version 1.2 of the subsystem schema.
 * </p>
 */
public class SecurityDomainModelv12UnitTestCase extends AbstractSubsystemBaseTest {

    private static String oldConfig;

    private static final char[] KEYSTORE_PASSWORD = "changeit".toCharArray();
    private static final char[] TRUSTSTORE_PASSWORD = "rmi+ssl".toCharArray();
    private static final String WORKING_DIRECTORY_LOCATION = "./target/test-classes";
    private static final String KEYSTORE_FILENAME = "clientcert.jks";
    private static final String TRUSTSTORE_FILENAME = "keystore.jks";
    private static final File KEY_STORE_FILE = new File(WORKING_DIRECTORY_LOCATION, KEYSTORE_FILENAME);
    private static final File TRUST_STORE_FILE = new File(WORKING_DIRECTORY_LOCATION, TRUSTSTORE_FILENAME);

    private static void createBlankKeyStores() throws Exception {
        File workingDir = new File(WORKING_DIRECTORY_LOCATION);
        if (workingDir.exists() == false) {
            workingDir.mkdirs();
        }

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(null, null);

        try (FileOutputStream fos = new FileOutputStream(KEY_STORE_FILE)){
            keyStore.store(fos, KEYSTORE_PASSWORD);
        }
        try (FileOutputStream fos = new FileOutputStream(TRUST_STORE_FILE)){
            trustStore.store(fos, TRUSTSTORE_PASSWORD);
        }
    }

    private static void deleteKeyStoreFiles() {
        File[] testFiles = {
                KEY_STORE_FILE,
                TRUST_STORE_FILE
        };
        for (File file : testFiles) {
            if (file.exists()) {
                file.delete();
            }
        }
    }

    @BeforeClass
    public static void beforeClass() {
        try {
            createBlankKeyStores();
            File target = new File(SecurityDomainModelv11UnitTestCase.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
            File config = new File(target, "config");
            config.mkdir();
            oldConfig = System.setProperty("jboss.server.config.dir", config.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void afterClass() {
        deleteKeyStoreFiles();
        if (oldConfig != null) {
            System.setProperty("jboss.server.config.dir", oldConfig);
        } else {
            System.clearProperty("jboss.server.config.dir");
        }
    }

    public SecurityDomainModelv12UnitTestCase() {
        super(SecurityExtension.SUBSYSTEM_NAME, new SecurityExtension());
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.ADMIN_ONLY_HC;
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("securitysubsystemv12.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-security_1_2.xsd";
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        super.compareXml(configId, original, marshalled, true);
    }

    @Override
    protected Properties getResolvedProperties() {
        Properties properties = new Properties();
        properties.put("jboss.server.config.dir", System.getProperty("java.io.tmpdir"));
        return properties;
    }

    @Test
    public void testOrder() throws Exception {
        KernelServices service = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("securitysubsystemv12.xml")
                .build();
        PathAddress address = PathAddress.pathAddress().append("subsystem", "security").append("security-domain", "ordering");
        address = address.append("authentication", "classic");

        ModelNode writeOp = Util.createOperation("write-attribute", address);
        writeOp.get("name").set("login-modules");
        for (int i = 1; i <= 6; i++) {
            ModelNode module = writeOp.get("value").add();
            module.get("code").set("module-" + i);
            module.get("flag").set("optional");
            module.get("module-options");

        }
        service.executeOperation(writeOp);
        ModelNode readOp = Util.createOperation("read-attribute", address);
        readOp.get("name").set("login-modules");
        ModelNode result = service.executeForResult(readOp);
        List<ModelNode> modules = result.asList();
        Assert.assertEquals("There should be exactly 6 modules but there are not", 6, modules.size());
        for (int i = 1; i <= 6; i++) {
            ModelNode module = modules.get(i - 1);
            Assert.assertEquals(module.get("code").asString(), "module-" + i);
        }
    }

}
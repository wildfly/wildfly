/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SHUTDOWN;
import static org.jboss.as.patching.Constants.MISC;
import static org.jboss.as.patching.HashUtils.hashFile;
import static org.jboss.as.patching.IoUtils.NO_CONTENT;
import static org.jboss.as.patching.IoUtils.newFile;
import static org.jboss.as.patching.metadata.ModificationType.ADD;
import static org.jboss.as.test.integration.domain.mixed.patching.PatchingTestUtil.createPatchXMLFile;
import static org.jboss.as.test.integration.domain.mixed.patching.PatchingTestUtil.createZippedPatchFile;
import static org.jboss.as.test.integration.domain.mixed.patching.PatchingTestUtil.dump;
import static org.jboss.as.test.integration.domain.mixed.patching.PatchingTestUtil.touch;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.process.protocol.StreamUtils;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.domain.management.util.WildFlyManagedConfiguration;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.version.ProductConfig;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.LocalModuleLoader;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests we can patch and rollback a legacy host by using the patch high level commands and management operations.
 *
 * This test is only valid for EAP 7.4.0 legacy hosts, since the patching subsystem was removed in EAP 8.0.0 and above
 */
public class PatchRemoteHostTest extends AbstractCliTestBase {
    private static final int EXIT_CODE_HOST_TIMEOUT = TimeoutUtil.adjust(30);
    protected static final PathAddress HOST_SECONDARY = PathAddress.pathAddress(HOST, "secondary");
    protected static final PathAddress CORE_SERVICE_PATCHING = PathAddress.pathAddress(CORE_SERVICE, "patching");
    private static final Path TARGET_DIR = Paths.get(System.getProperty("basedir", ".")).resolve("target");
    private static DomainLifecycleUtil primaryLifecycleUtil;
    private static DomainLifecycleUtil secondaryLifecycleUtil;
    private static MixedDomainTestSupport support;
    private static Path tempDir;
    private static ProductVersion secondaryProductVersion;

    @Before
    public void init() throws Exception {
        Assert.assertNotNull("This class is designed to be invoked from a parent test case that utilizes the @Version annotation to specify the target server version.", support);
        tempDir = Files.createTempDirectory(TARGET_DIR, "patch-remote-host-test-");
    }

    @After
    public void cleanup() throws Exception {
        if (tempDir != null && tempDir.toFile().exists()) {
            try (Stream<Path> walk = Files.walk(tempDir)) {
                walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    public static void setup(Class<?> testClass) throws IOException {
        support = MixedDomainTestSuite.getSupport(testClass);

        primaryLifecycleUtil = support.getDomainPrimaryLifecycleUtil();
        secondaryLifecycleUtil = support.getDomainSecondaryLifecycleUtil();

        WildFlyManagedConfiguration configuration = secondaryLifecycleUtil.getConfiguration();
        secondaryProductVersion = new ProductVersion(configuration);
    }

    /**
     * Tests we can patch and rollback a legacy host by using the patch high level commands using
     * a legacy host client. The operation should be rejected by using a client connected to the DC.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testPatchCommand() throws Exception {
        try {
            AbstractCliTestBase.initCLI(DomainTestSupport.primaryAddress);
            // sanity check to validate we are connected to the primary DC and not to the secondary one
            final String primaryHostName = primaryLifecycleUtil.getConfiguration().getHostName();
            final String secondaryHostName = secondaryLifecycleUtil.getConfiguration().getHostName();

            Assert.assertTrue(cli.sendLine(" :query(select=[\"host\"])", false));
            String response = cli.readOutput();
            Assert.assertTrue(response.contains(primaryHostName));
            Assert.assertTrue(response.contains(secondaryHostName));

            // Patch high level commands are no longer available against primary host
            AssertionError exception = assertThrows(AssertionError.class, () -> {
                cli.sendLine("patch info --host=" + primaryHostName);
            });

            String expectedMessage = "command is not supported on this host controller";
            String actualMessage = exception.getMessage();
            assertTrue(actualMessage, actualMessage.contains(expectedMessage));

            // works against the secondary if the host is an EAP 7.4.0
            Assert.assertTrue(cli.sendLine("patch info --host=" + secondaryHostName, false));

            final String patchID = "secondary-host-domain-patch";
            final File patch = createOneOffPatchAddingMiscFile(patchID, secondaryProductVersion.getVersion());

            cli.sendLine("patch apply " + patch.getAbsolutePath() + " --host=" + secondaryHostName);
            cli.close();
            cli = null;

            // Restart the secondary
            final ModelControllerClient client = primaryLifecycleUtil.getDomainClient();
            restartSecondary(client);

            // Connect CLI again and verify the patch was installed
            AbstractCliTestBase.initCLI(DomainTestSupport.primaryAddress);
            Assert.assertTrue(cli.sendLine("patch info --patch-id=" + patchID + " --host=" + secondaryHostName, false));

            // rollback the latest patch
            Assert.assertTrue(cli.sendLine("patch rollback --reset-configuration=false --host=" + secondaryHostName, false));
            cli.close();
            cli = null;

            // restart the secondary
            restartSecondary(client);

            // Connect CLI again and verify the patch is no longer installed
            AbstractCliTestBase.initCLI(DomainTestSupport.primaryAddress);
            // Patch high level commands are no longer available against primary host
            exception = assertThrows(AssertionError.class, () -> {
                cli.sendLine("patch info --patch-id=" + patchID + " --host=" + secondaryHostName);
            });

            //"WFLYPAT0021: Patch 'secondary-host-domain-patch' not found in history."
            expectedMessage = "WFLYPAT0021:";
            actualMessage = exception.getMessage();

            assertTrue(actualMessage, actualMessage.contains(expectedMessage));
        } finally {
            if (cli.isConnected()) {
                cli.close();
            }
        }
    }

    /**
     * Tests we can patch and rollback a legacy host from the DC by using the patch management operation
     * against the secondary host resource.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testPatchMgmtOperation() throws Exception {
        final ModelControllerClient client = primaryLifecycleUtil.getDomainClient();

        final ModelNode patchOp = new ModelNode();
        patchOp.get(OP).set("patch");
        patchOp.get(OP_ADDR).set(HOST_SECONDARY.append(CORE_SERVICE_PATCHING).toModelNode());

        final String patchID = "simple-domain-patch";
        final File patch = createOneOffPatchAddingMiscFile(patchID, secondaryProductVersion.getVersion());
        final Operation op = OperationBuilder.create(patchOp).addFileAsAttachment(patch).build();

        try {
            DomainTestUtils.executeForResult(op, client);
        } finally {
            StreamUtils.safeClose(op);
        }

        // Restart the secondary
        restartSecondary(client);

        final ModelNode patchesOp = new ModelNode();
        patchesOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
        patchesOp.get(OP_ADDR).set(HOST_SECONDARY.append(CORE_SERVICE_PATCHING).toModelNode());
        patchesOp.get(NAME).set("patches");

        // Check the applied patch
        final ModelNode entry = new ModelNode().set(patchID);
        Assert.assertTrue(DomainTestUtils.executeForResult(patchesOp, client).asList().contains(entry));

        // Rollback
        final ModelNode rollback = new ModelNode();
        rollback.get(OP).set("rollback");
        rollback.get(OP_ADDR).set(HOST_SECONDARY.append(CORE_SERVICE_PATCHING).toModelNode());
        rollback.get("patch-id").set(patchID);
        rollback.get("reset-configuration").set(false);
        DomainTestUtils.executeForResult(rollback, client);

        // Restart
        restartSecondary(client);

        // Check there is no patch applied
        Assert.assertTrue(DomainTestUtils.executeForResult(patchesOp, client).asList().isEmpty());
    }

    static class ProductVersion {
        private final String product;
        private final String version;

        public ProductVersion(WildFlyManagedConfiguration configuration) throws IOException {
            final Path jbossHomeDir = Paths.get(configuration.getJbossHome());
            final Path modulesRoot = jbossHomeDir.resolve("modules")
                    .resolve("system")
                    .resolve("layers")
                    .resolve("base")
                    .toAbsolutePath();

            // Load the current product conf
            final LocalModuleLoader loader = new LocalModuleLoader(new File[]{modulesRoot.toFile()});
            try {
                final Module module = loader.loadModule("org.jboss.as.version");

                final Class<?> clazz = module.getClassLoader().loadClass("org.jboss.as.version.ProductConfig");
                final Method resolveName = clazz.getMethod("resolveName");
                final Method resolveVersion = clazz.getMethod("resolveVersion");
                final Method fromFilesystemSlot = clazz.getMethod("fromFilesystemSlot", ModuleLoader.class, String.class, Map.class);

                final Object productConfig = fromFilesystemSlot.invoke(null, loader, jbossHomeDir.toAbsolutePath().toString(),
                        Collections.emptyMap());

                product = (String) resolveName.invoke(productConfig);
                version = (String) resolveVersion.invoke(productConfig);
            } catch (Exception e) {
                throw new RuntimeException(modulesRoot.toString(), e);
            }
        }

        public String getProduct() {
            return product;
        }

        public String getVersion() {
            return version;
        }
    }

    void restartSecondary(final ModelControllerClient client) throws Exception {
        final ModelNode restart = new ModelNode();
        restart.get(OP).set(SHUTDOWN);
        restart.get(OP_ADDR).set(HOST_SECONDARY.toModelNode());
        restart.get(RESTART).set(true);

        DomainTestUtils.executeForResult(restart, client);
        secondaryLifecycleUtil.awaitForProcessExitCode(EXIT_CODE_HOST_TIMEOUT);
        secondaryLifecycleUtil.start();
        secondaryLifecycleUtil.awaitHostController(System.currentTimeMillis());
    }

    private File createOneOffPatchAddingMiscFile(String patchID, String asVersion) throws Exception {
        File oneOffPatchDir = Files.createDirectories(tempDir.resolve(patchID)).toFile();
        ContentModification miscFileAdded = addMisc(oneOffPatchDir, patchID, "test content", "awesomeDirectory", "awesomeFile");
        ProductConfig productConfig = new ProductConfig(secondaryProductVersion.getProduct(), asVersion, "main");
        Patch oneOffPatch = PatchBuilder.create().setPatchId(patchID).setDescription("A one-off patch adding a misc file.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion()).getParent()
                .addContentModification(miscFileAdded).build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        return createZippedPatchFile(oneOffPatchDir, patchID);
    }

    public static ContentModification addMisc(File patchDir, String patchElementID, String content, String... fileSegments)
            throws IOException {
        File miscDir = newFile(patchDir, patchElementID, MISC);
        File addedFile = touch(miscDir, fileSegments);
        dump(addedFile, content);
        byte[] newHash = hashFile(addedFile);
        String[] subdir = new String[fileSegments.length - 1];
        System.arraycopy(fileSegments, 0, subdir, 0, fileSegments.length - 1);
        return new ContentModification(new MiscContentItem(addedFile.getName(), subdir, newHash),
                NO_CONTENT, ADD);
    }
}

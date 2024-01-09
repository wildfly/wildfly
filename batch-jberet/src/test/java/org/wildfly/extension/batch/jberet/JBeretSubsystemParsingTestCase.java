/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.subsystem.test.LegacyKernelServicesInitializer;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.wildfly.extension.batch.jberet.job.repository.CommonAttributes;
import org.wildfly.extension.batch.jberet.job.repository.InMemoryJobRepositoryDefinition;
import org.wildfly.extension.batch.jberet.job.repository.JdbcJobRepositoryDefinition;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Basic subsystem test. Tests parsing various batch configurations
 */
@SuppressWarnings("deprecation")
public class JBeretSubsystemParsingTestCase extends AbstractBatchTestCase {

    public JBeretSubsystemParsingTestCase() {
        super(BatchSubsystemDefinition.NAME, new BatchSubsystemExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("/default-subsystem.xml");
    }

    @Override
    protected String getSubsystemXsdPath() {
        return "schema/wildfly-batch-jberet_4_0.xsd";
    }

    @Test
    public void testMinimalSubsystem() throws Exception {
        standardSubsystemTest("/minimal-subsystem.xml");
    }

    @Test
    public void testMultiThreadFactory() throws Exception {
        standardSubsystemTest("/multi-thread-factory-subsystem.xml");
    }

    @Test
    public void testJdbcSubsystem() throws Exception {
        standardSubsystemTest("/jdbc-default-subsystem.xml");
    }
    @Test
    public void testJpaSubsystem() throws Exception {
        standardSubsystemTest("/jpa-default-subsystem.xml");
    }

    @Test
    public void testSecurityDomainSubsystem() throws Exception {
        standardSubsystemTest("/security-domain-subsystem.xml");
    }

    /**
     * Verifies that attributes with expression are handled properly.
     *
     * @throws Exception for any test failure
     */
    @Test
    public void testExpressionInAttributeValue() throws Exception {
        final KernelServices kernelServices = boot(getSubsystemXml("/with-expression-subsystem.xml"));
        final ModelNode batchModel = kernelServices.readWholeModel().get("subsystem", getMainSubsystemName());
        final boolean expectedRestartOnResume = false;
        final boolean restartOnResume = batchModel.get("restart-jobs-on-resume").resolve().asBoolean();
        assertEquals("Expecting restart-jobs-on-resume " + expectedRestartOnResume + ", but got " + restartOnResume,
                expectedRestartOnResume, restartOnResume);

        final ModelNode threadPool = batchModel.get("thread-pool").asProperty().getValue();
        final int expectedMaxThreads = 10;
        final int maxThreads = threadPool.get("max-threads").resolve().asInt();
        assertEquals("Expecting max-threads " + expectedMaxThreads + ", but got " + maxThreads,
                expectedMaxThreads, maxThreads);

        final ModelNode threadFactory = batchModel.get("thread-factory").asProperty().getValue();
        final String expectedGroupName = "batch";
        final String groupName = threadFactory.get("group-name").resolve().asString();
        assertEquals("Expecting thread-factory group-name " + expectedGroupName + ", but got " + groupName,
                expectedGroupName, groupName);

        final int expectedPriority = 5;
        final int priority = threadFactory.get("priority").resolve().asInt();
        assertEquals("Expecting thread-factory priority " + expectedPriority + ", but got " + priority,
                expectedPriority, priority);

        final String expectedThreadNamePattern = "%i-%g";
        final String threadNamePattern = threadFactory.get("thread-name-pattern").resolve().asString();
        assertEquals("Expecting thread-factory thread-name-pattern " + expectedThreadNamePattern + ", but got " + threadNamePattern,
                expectedThreadNamePattern, threadNamePattern);
    }

    @Test
    public void testLegacySubsystems() throws Exception {
        // Get a list of all the logging_x_x.xml files
        final Pattern pattern = Pattern.compile("(.*-subsystem)_\\d+_\\d+\\.xml");
        // Using the CP as that's the standardSubsystemTest will use to find the config file
        final String cp = WildFlySecurityManager.getPropertyPrivileged("java.class.path", ".");
        final String[] entries = cp.split(Pattern.quote(File.pathSeparator));
        final List<String> configs = new ArrayList<>();
        for (String entry : entries) {
            final Path path = Paths.get(entry);
            if (Files.isDirectory(path)) {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                        final String name = file.getFileName().toString();
                        if (pattern.matcher(name).matches()) {
                            configs.add("/" + name);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }

        // The paths shouldn't be empty
        assertFalse("No configs were found", configs.isEmpty());

        for (String configId : configs) {
            // Run the standard subsystem test, but don't compare the XML as it should never match
            standardSubsystemTest(configId, false);
        }
    }

    @Test
    public void testRejectingTransformersEAP74() throws Exception {
        FailedOperationTransformationConfig transformationConfig = new FailedOperationTransformationConfig();

        PathAddress repositoryAddress = PathAddress.pathAddress(BatchSubsystemDefinition.SUBSYSTEM_PATH, InMemoryJobRepositoryDefinition.PATH);
        transformationConfig.addFailedAttribute(repositoryAddress,
                new FailedOperationTransformationConfig.NewAttributesConfig(CommonAttributes.EXECUTION_RECORDS_LIMIT));

        PathAddress jdbcRepositoryAddress = PathAddress.pathAddress(BatchSubsystemDefinition.SUBSYSTEM_PATH, JdbcJobRepositoryDefinition.PATH);
        transformationConfig.addFailedAttribute(jdbcRepositoryAddress,
                new FailedOperationTransformationConfig.NewAttributesConfig(CommonAttributes.EXECUTION_RECORDS_LIMIT));

        testRejectingTransformers(transformationConfig, ModelTestControllerVersion.EAP_7_4_0);
    }

    private void testRejectingTransformers(FailedOperationTransformationConfig transformationConfig, ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion subsystemModelVersion = controllerVersion.getSubsystemModelVersion(BatchSubsystemDefinition.NAME);

        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        LegacyKernelServicesInitializer kernelServicesInitializer = builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, subsystemModelVersion)
                .addMavenResourceURL("org.wildfly.core:wildfly-threads:" + controllerVersion.getCoreVersion())
                .dontPersistXml();
        try {
            BatchSubsystemExtension.class.getClassLoader().loadClass("javax" + ".batch.operations.JobStartException");
            kernelServicesInitializer.addMavenResourceURL("org.jboss.eap:wildfly-batch-jberet:" + controllerVersion.getMavenGavVersion());
        } catch (ClassNotFoundException e) {
            kernelServicesInitializer.addMavenResourceURL("org.wildfly:wildfly-batch-jberet-jakarta:26.0.0.Final");
        }
        KernelServices kernelServices = builder.build();
        assertTrue(kernelServices.isSuccessfulBoot());
        assertTrue(kernelServices.getLegacyServices(subsystemModelVersion).isSuccessfulBoot());

        List<ModelNode> operations = builder.parseXmlResource("/default-subsystem.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(kernelServices, subsystemModelVersion, operations, transformationConfig);
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities(
                "org.wildfly.data-source.ExampleDS",
                "org.wildfly.security.security-domain.ApplicationDomain",
                "org.wildfly.transactions.global-default-local-provider");
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.batch.jberet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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

import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Basic subsystem test. Tests parsing various batch configurations
 */
public class JBeretSubsystemParsingTestCase extends AbstractBatchTestCase {

    public JBeretSubsystemParsingTestCase() {
        super(BatchSubsystemDefinition.NAME, new BatchSubsystemExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("/default-subsystem.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-batch-jberet_3_0.xsd";
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
    public void testSecurityDomainSubsystem() throws Exception {
        standardSubsystemTest("/security-domain-subsystem.xml");
    }

    /**
     * Verifies that attributes with expression are handled properly.
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
                    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
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

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities(
                "org.wildfly.data-source.ExampleDS",
                "org.wildfly.security.security-domain.ApplicationDomain",
                "org.wildfly.transactions.global-default-local-provider");
    }
}

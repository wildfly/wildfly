/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2015, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */
package org.jboss.as.ee.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.ee.subsystem.EESubsystemModel.ANNOTATION_PROPERTY_REPLACEMENT;
import static org.jboss.as.ee.subsystem.EESubsystemModel.EAR_SUBDEPLOYMENTS_ISOLATED;
import static org.jboss.as.ee.subsystem.EESubsystemModel.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT;
import static org.jboss.as.ee.subsystem.EESubsystemModel.SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.ANNOTATIONS;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.GLOBAL_MODULES;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.META_INF;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.NAME;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.SERVICES;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.SLOT;
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

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="opalka.richard@gmail.com">Richard Opalka</a>
 */
public class EeLegacySubsystemTestCase extends AbstractSubsystemBaseTest {

    public EeLegacySubsystemTestCase() {
        super(EeExtension.SUBSYSTEM_NAME, new EeExtension());
    }

    @Test
    public void testLegacyConfigurations() throws Exception {
        // Get a list of all the logging_x_x.xml files
        final Pattern pattern = Pattern.compile("(subsystem)_\\d+_\\d+\\.xml");
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
                            configs.add(name);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }

        // The paths shouldn't be empty
        Assert.assertFalse("No configs were found", configs.isEmpty());

        for (String configId : configs) {
            // Run the standard subsystem test, but don't compare the XML as it should never match
            standardSubsystemTest(configId, false);
        }
    }

    @Test
    public void testSubsystem() throws Exception {
        KernelServices services = standardSubsystemTest("subsystem_1_2.xml", false);
        ModelNode model = services.readWholeModel();
        ModelNode subsystem = model.require(SUBSYSTEM).require(EeExtension.SUBSYSTEM_NAME);
        ModelNode globalModules = subsystem.require(GLOBAL_MODULES);

        assertEquals("org.jboss.logging", globalModules.require(0).require(NAME).asString());
        assertEquals("main", globalModules.require(0).require(SLOT).asString());

        assertEquals("org.apache.log4j", globalModules.require(1).require(NAME).asString());
        assertTrue(globalModules.require(1).require(ANNOTATIONS).asBoolean());
        assertTrue(globalModules.require(1).require(META_INF).asBoolean());
        assertFalse(globalModules.require(1).require(SERVICES).asBoolean());

        assertFalse(subsystem.require(ANNOTATION_PROPERTY_REPLACEMENT).asBoolean());
        assertTrue(subsystem.require(EAR_SUBDEPLOYMENTS_ISOLATED).asBoolean());
        assertTrue(subsystem.require(JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT).asBoolean());
        assertFalse(subsystem.require(SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT).asBoolean());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_1_2.xml");
    }

    @Override
    protected String getSubsystemXml(String resource) throws IOException {
        return readResource(resource);
    }

    @Override
    public void testSchema() throws Exception {
        //This test is testing the legacy schemas, for which this test is not relevant.
    }
}

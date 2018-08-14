/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.config.smallrye.converter;

import java.io.File;
import java.net.URL;

import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Add a config-source with a custom class in the microprofile-config subsystem.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class SetupTask implements ServerSetupTask {

    private static final String TEST_MODULE_NAME = "test.custom-config-converters";

    private static TestModule testModule;

    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        final File archiveDir = new File("target/archives");
        archiveDir.mkdirs();
        File moduleFile = new File(archiveDir, "config-converters.jar");
        JavaArchive configSourceServiceLoad = ShrinkWrap.create(JavaArchive.class, "config-converters.jar")
                .addClasses(Return101Converter.class, Return102Converter.class, HighPriorityStringConverter1.class,
                        HighPriorityStringConverter2.class)
                .addAsServiceProvider(Converter.class, Return101Converter.class, Return102Converter.class,
                        HighPriorityStringConverter1.class, HighPriorityStringConverter2.class);
        configSourceServiceLoad.as(ZipExporter.class).exportTo(moduleFile);
        URL url = MicroProfileConfigConvertersTestCase.class.getResource("module.xml");
        File moduleXmlFile = new File(url.toURI());
        testModule = new TestModule(TEST_MODULE_NAME, moduleXmlFile);
        testModule.addJavaArchive(moduleFile);
        testModule.create();
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        testModule.remove();
        final File archiveDir = new File("target/archives");
        cleanFile(archiveDir);
    }

    private static void cleanFile(File toClean) {
        if (toClean.exists()) {
            if (toClean.isDirectory()) {
                for (File child : toClean.listFiles()) {
                    cleanFile(child);
                }
            }
            toClean.delete();
        }
    }

}

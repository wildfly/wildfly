/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.converter;

import java.io.File;
import java.io.FileOutputStream;
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
                .addClasses(Return101Converter.class, Return102Converter.class, HighPriorityMyStringConverter1.class,
                        HighPriorityMyStringConverter2.class, MyString.class)
                .addAsServiceProvider(Converter.class, Return101Converter.class, Return102Converter.class,
                        HighPriorityMyStringConverter1.class, HighPriorityMyStringConverter2.class);
        URL url = MicroProfileConfigConvertersTestCase.class.getResource("module.xml");
        File moduleXmlFile = new File(url.toURI());
        try (FileOutputStream target = new FileOutputStream(moduleFile)) {
            configSourceServiceLoad.as(ZipExporter.class).exportTo(target);
        }
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

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.compression;

import java.io.File;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

public abstract class AbstractReactiveMessagingKafkaWithNativeCompressionFailsOnWindowsAndMacTestCase extends AbstractCliTestBase {
    @ArquillianResource
    public ManagementClient managementClient;

    private final String propertiesFileName;

    protected AbstractReactiveMessagingKafkaWithNativeCompressionFailsOnWindowsAndMacTestCase(String propertiesFileName) {
        this.propertiesFileName = propertiesFileName;
    }

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "dummy.jar");
    }

    @BeforeClass
    public static void before() throws Exception {
        AbstractCliTestBase.initCLI();
    }

    @AfterClass
    public static void after() throws Exception {
        AbstractCliTestBase.closeCLI();
    }


    @Test
    public void testDeploymentFailsOnWindowsAndMac() {
        // Only check the deployment fails on Windows or Mac if the config is to not allow Snappy
        Assume.assumeFalse(ReactiveMessagingKafkaCompressionTestCase.isNativeCompressionEnabled());

        // We need the deployment with Snappy enabled
        WebArchive archive = ReactiveMessagingKafkaCompressionTestCase.getDeploymentWithNativeCompressionEnabled(propertiesFileName);
        File file = new File("target/rm-kafka-with-snappy.war");
        archive.as(ZipExporter.class).exportTo(file, true);
        try {
            boolean deployed = false;
            try {
                cli.sendLine("deploy " + file.getAbsolutePath());
                deployed = true;
                Assert.fail("Should not have been able to deploy the jar");
            } catch (Throwable error) {
                if (error.getMessage().contains("WFLYRXMKAF0003")) {
                    // This is expected since the deployment contains Snappy compression.
                    // We don't support that in this setup, so the deployer will throw an error
                    // with this logger message id
                } else {
                    throw error;
                }
            } finally {
                if (deployed) {
                    cli.sendLine("undeploy " + file.getName());
                }
            }
        } finally {
            file.delete();
        }
    }

}

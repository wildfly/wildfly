/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.compression;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testcontainers.api.DockerRequired;
import org.jboss.as.arquillian.api.ServerSetup;
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
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.RunKafkaSetupTask;

import java.io.File;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({RunKafkaSetupTask.class, EnableReactiveExtensionsSetupTask.class})
@DockerRequired
public class ReactiveMessagingKafkaWithSnappyFailsOnWindowsAndMacTestCase extends AbstractCliTestBase {
    @ArquillianResource
    public ManagementClient managementClient;

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
        Assume.assumeFalse(ReactiveMessagingKafkaCompressionTestCase.isSnappyEnabled());

        // We need the deployment with Snappy enabled
        WebArchive archive = ReactiveMessagingKafkaCompressionTestCase.getDeploymentWithSnappyEnabled();
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

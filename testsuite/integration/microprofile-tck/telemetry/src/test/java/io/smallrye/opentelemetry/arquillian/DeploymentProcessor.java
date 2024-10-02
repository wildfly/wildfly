/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.smallrye.opentelemetry.arquillian;

import java.io.File;

import io.smallrye.opentelemetry.ExceptionMapper;
import io.smallrye.opentelemetry.TestConfigSource;
import jakarta.ws.rs.ext.Providers;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author Pavol Loffay
 */
public class DeploymentProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        if (archive instanceof WebArchive) {
            JavaArchive extensionsJar = ShrinkWrap.create(JavaArchive.class, "extension.jar")
                    .addClasses(ExceptionMapper.class, TestConfigSource.class)
                    .addAsServiceProvider(ConfigSource.class, TestConfigSource.class)
                    .addAsServiceProvider(Providers.class, ExceptionMapper.class);

            WebArchive war = WebArchive.class.cast(archive);
            war
                    .addAsLibraries(extensionsJar)
                    .addAsManifestResource(new StringAsset("telemetry.tck.executor=telemetry.tck.executor=java.util.concurrent.ForkJoinPool"),
                            "microprofile-telemetry-tck.properties")
                    .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"/>"), "beans.xml");

            final File archiveDir = new File("target/archives");
            archiveDir.mkdirs();
            File moduleFile = new File(archiveDir, "testapp.war");
            war.as(ZipExporter.class).exportTo(moduleFile, true);
        }
    }
}

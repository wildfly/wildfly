/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.compression;

import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.RunKafkaSetupTask;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@ServerSetup({RunKafkaSetupTask.class, EnableReactiveExtensionsSetupTask.class})
public class ReactiveMessagingKafkaCompressionTestCase {

    private static final long TIMEOUT = TimeoutUtil.adjust(15000);

    @Inject
    CompressionMessagingBean bean;


    @Deployment
    public static WebArchive getDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "reactive-messaging-kafka-tx.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackage(ReactiveMessagingKafkaCompressionTestCase.class.getPackage())
                .addClasses(RunKafkaSetupTask.class, EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class)
                .addAsWebInfResource(ReactiveMessagingKafkaCompressionTestCase.class.getPackage(), "microprofile-config.properties", "classes/META-INF/microprofile-config.properties")
                .addClass(TimeoutUtil.class)
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                        new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")
                ), "permissions.xml");

        webArchive.as(ZipExporter.class).exportTo(new File("target/test-original.war"), true);

        return webArchive;
    }

    @Test
    public void test() throws InterruptedException {
        bean.sendGzip("Hello");
        bean.sendSnappy("World");
        bean.sendLz4("of");
        bean.sendZstd("Reactive");

        boolean wait = bean.getLatch().await(TIMEOUT, TimeUnit.MILLISECONDS);
        Assert.assertTrue("Timed out", wait);
        Set<String> expected = new HashSet<>(Arrays.asList("Hello", "World", "of", "Reactive"));
        Assert.assertEquals(expected.size(), bean.getWords().size());
        Assert.assertTrue("Expected " + bean.getWords() + " to contain all of " + expected, bean.getWords().containsAll(expected));

    }
}

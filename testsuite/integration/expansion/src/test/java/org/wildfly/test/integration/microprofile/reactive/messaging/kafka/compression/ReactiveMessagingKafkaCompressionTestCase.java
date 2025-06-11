/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.compression;

import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context.KafkaClientCustomizer;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.RunKafkaSetupTask;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@ServerSetup({RunKafkaSetupTask.class, EnableReactiveExtensionsSetupTask.class})
@TestcontainersRequired
public class ReactiveMessagingKafkaCompressionTestCase {

    // Downstream we want to disable Snappy on Windows and Mac
    // This setting should match that in KafkaClientCustomizer.DISABLE_SNAPPY_ON_WINDOWS_AND_MAC
    // I don't want to depend on that module from here
    static final boolean DISABLE_NATIVE_COMPRESSION_ON_WINDOWS_AND_MAC =
            KafkaClientCustomizer.DISABLE_NATIVE_COMPRESSION_ON_WINDOWS_AND_MAC;

    private static final long TIMEOUT = TimeoutUtil.adjust(15000);

    @Inject
    CompressionMessagingBean bean;

    static boolean isNativeCompressionEnabled() {
        String os = WildFlySecurityManager.getPropertyPrivileged("os.name", "x").toLowerCase(Locale.ENGLISH);
        boolean runningOnWindowsOrMac = os.startsWith("windows") || os.startsWith("mac os");
        return  !runningOnWindowsOrMac || !DISABLE_NATIVE_COMPRESSION_ON_WINDOWS_AND_MAC;
    }

    @Deployment
    public static WebArchive getDeployment() {
        boolean enableSnappy = isNativeCompressionEnabled();
        String mpConfigFile = enableSnappy ? "microprofile-config.properties" : "microprofile-config-no-native-compression.properties";
        return getDeploymentInternal(mpConfigFile);
    }

    static WebArchive getDeploymentWithNativeCompressionEnabled(String propertiesFileName) {
        // Don't choose here whether to use Snappy or not
        return getDeploymentInternal(propertiesFileName);
    }

    static WebArchive getDeploymentInternal(String mpConfigFile) {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "reactive-messaging-kafka-tx.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackage(ReactiveMessagingKafkaCompressionTestCase.class.getPackage())
                .addClasses(RunKafkaSetupTask.class, EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class)
                .addAsWebInfResource(ReactiveMessagingKafkaCompressionTestCase.class.getPackage(), mpConfigFile, "classes/META-INF/microprofile-config.properties")
                .addClass(TimeoutUtil.class)
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                        new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")
                ), "permissions.xml");

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

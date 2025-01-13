/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.mp.tck.reactive.streams.operators;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.lang.reflect.ReflectPermission;
import java.util.PropertyPermission;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.container.ManifestContainer;

/**
 * @author Radoslav Husar
 */
public class ReactiveStreamsOperatorsApplicationArchiveProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        /*
        if (!(applicationArchive instanceof ClassContainer<?>)) {
            return;
        }
        ClassContainer<?> classContainer = (ClassContainer<?>) applicationArchive;

        if (applicationArchive instanceof LibraryContainer) {
            JavaArchive additionalBeanArchive = ShrinkWrap.create(JavaArchive.class);
            additionalBeanArchive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
            ((LibraryContainer<?>) applicationArchive).addAsLibrary(additionalBeanArchive);
        } else {
            classContainer.addAsResource(EmptyAsset.INSTANCE, "META-INF/beans.xml");
        }

        if (!applicationArchive.contains("META-INF/beans.xml")) {
            applicationArchive.add(EmptyAsset.INSTANCE, "META-INF/beans.xml");
        }
*/

        if (applicationArchive instanceof ManifestContainer<?>) {
            ManifestContainer<?> manifestContainer = (ManifestContainer<?>) applicationArchive;

            // Run the TCK with security manager
            manifestContainer.addAsManifestResource(createPermissionsXmlAsset(
                    // Permissions required by test instrumentation - arquillian-core.jar and arquillian-testng.jar
                    new ReflectPermission("suppressAccessChecks"),
                    new PropertyPermission("*", "read,write"),
                    new RuntimePermission("getenv.*"),
                    new RuntimePermission("modifyThread"),
                    new RuntimePermission("accessDeclaredMembers"),
                    // Permissions required by test instrumentation - awaitility.jar
                    new RuntimePermission("setDefaultUncaughtExceptionHandler"),
                    new RuntimePermission("modifyThread"),

                    new java.lang.RuntimePermission("getClassLoader")
            ), "permissions.xml");
        }
    }

}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.openapi.tck;

import static org.wildfly.testing.tools.deployments.DeploymentDescriptors.createPermissionsXmlAsset;

import java.lang.reflect.ReflectPermission;
import java.util.PropertyPermission;

import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.hamcrest.internal.ReflectiveTypeFinder;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.container.ManifestContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class DeploymentProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        if (applicationArchive instanceof WebArchive) {
            JavaArchive extensionsJar = ShrinkWrap.create(JavaArchive.class, "extension.jar");

            extensionsJar.addPackage(Matchers.class.getPackage());
            extensionsJar.addPackage(IsEmptyCollection.class.getPackage());
            extensionsJar.addPackage(IsEqual.class.getPackage());
            extensionsJar.addPackage(ReflectiveTypeFinder.class.getPackage());

            WebArchive war = (WebArchive) applicationArchive;
            war.addAsLibraries(extensionsJar);
        }

        if (applicationArchive instanceof ManifestContainer<?>) {
            ManifestContainer<?> manifestContainer = (ManifestContainer<?>) applicationArchive;

            // Enable running the TCK with the security manager
            manifestContainer.addAsManifestResource(createPermissionsXmlAsset(
                    // Permissions required by test instrumentation - arquillian-core.jar and arquillian-testng.jar
                    new ReflectPermission("suppressAccessChecks"),
                    new PropertyPermission("*", "read"),
                    new RuntimePermission("accessDeclaredMembers")
            ), "permissions.xml");
        }
    }
}
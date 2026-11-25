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
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.container.ManifestContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Enhances testable and non-testable deployments.
 * @author Paul Ferraro
 */
public class DeploymentEnhancer {

    public void beforeDeploy(@Observes BeforeDeploy event) {
        Archive<?> archive = event.getDeployment().getArchive();
        if (archive instanceof WebArchive) {
            // Add dependency jar containing required hamcrest classes
            JavaArchive extensionsJar = ShrinkWrap.create(JavaArchive.class, "extension.jar");

            extensionsJar.addPackage(Matchers.class.getPackage());
            extensionsJar.addPackage(IsEmptyCollection.class.getPackage());
            extensionsJar.addPackage(IsEqual.class.getPackage());
            extensionsJar.addPackage(ReflectiveTypeFinder.class.getPackage());

            WebArchive war = (WebArchive) archive;
            war.addAsLibraries(extensionsJar);
            // As of WFLY-20567, test assumptions require use of the root context
            war.addAsWebInfResource(DeploymentEnhancer.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        }

        if (archive instanceof ManifestContainer<?>) {
            ManifestContainer<?> manifestContainer = (ManifestContainer<?>) archive;

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
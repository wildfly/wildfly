/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.config.test;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.util.PropertyPermission;

import org.jboss.arquillian.container.spi.client.container.DeploymentExceptionTransformer;
import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentScenario;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.container.ManifestContainer;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2019 Red Hat inc.
 */
public class WildFlyArquillianExtension implements LoadableExtension {
    @Override
    public void register(ExtensionBuilder extensionBuilder) {
        extensionBuilder.service(ApplicationArchiveProcessor.class, DeploymentProcessor.class);
        extensionBuilder.service(DeploymentExceptionTransformer.class, WildFlyDeploymentExceptionTransformer.class);
        extensionBuilder.observer(PermissionsDeploymentScenarioObserver.class);
    }

    public static class PermissionsDeploymentScenarioObserver {
        public void addPermissions(@Observes DeploymentScenario deploymentScenario) {
            for (Deployment deployment : deploymentScenario.deployments()) {
                DeploymentDescription description = deployment.getDescription();
                Archive<?> testArchive = description.getTestableArchive();

                // We need to add the permissions.xml here rather than the normal approach, which would
                // be in an ApplicationArchiveProcessor.
                // The normal approach adds permissions to the application archive. However, since that
                // is a .jar and we are using the Servlet Protocol for in-server testing, Arquillian
                // creates a test.war containing the original .jar. The permissions.xml from the now nested
                // original .jar is not picked up by PermissionsParserProcessor so the permissions are ignored.
                // testArchive is the testable .war, so we add the permissions there instead.

                if (testArchive instanceof ManifestContainer<?>) {
                    ManifestContainer<?> manifestContainer = (ManifestContainer<?>) testArchive;

                    manifestContainer.addAsManifestResource(createPermissionsXmlAsset(
                            // Permissions required by test instrumentation - arquillian-core.jar and arquillian-testng.jar
                            new ReflectPermission("suppressAccessChecks"),
                            new PropertyPermission("*", "read,write"),
                            new RuntimePermission("accessDeclaredMembers"),
                            new RuntimePermission("getClassLoader"),
                            // Permissions required by the MP Config implementation
                            // Needed until https://redhat.atlassian.net/browse/WFLY-21849 is fixed
                            new FilePermission("<<ALL FILES>>", "read"),
                            new RuntimePermission("getenv.*")
                    ), "permissions.xml");
                }

                description.setTestableArchive(testArchive);
            }
        }
    }
}

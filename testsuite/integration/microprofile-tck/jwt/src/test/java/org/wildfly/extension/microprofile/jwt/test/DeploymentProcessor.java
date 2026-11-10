/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.jwt.test;

import static org.wildfly.testing.tools.deployments.DeploymentDescriptors.createPermissionsXmlAsset;

import java.io.File;
import java.io.FilePermission;
import java.net.SocketPermission;
import java.security.Permission;
import java.util.HashSet;
import java.util.Set;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.container.ManifestContainer;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Class to handle additional processing needed for deployments, specifically microprofile-config.properties, web.xml,
 * jboss-web.xml, and permissions.xml.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Radoslav Husar
 */
public class DeploymentProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        if (applicationArchive instanceof WebArchive) {
            WebArchive war = (WebArchive) applicationArchive;
            if (!war.contains("META-INF/microprofile-config.properties")) {
                war.addAsManifestResource("META-INF/microprofile-config-local.properties", "microprofile-config.properties");
            }
            war.addAsWebInfResource("WEB-INF/web.xml", "web.xml");
            war.addAsWebInfResource("WEB-INF/jboss-web.xml", "jboss-web.xml");
        }

        if (applicationArchive instanceof ManifestContainer<?>) {
            ManifestContainer<?> manifestContainer = (ManifestContainer<?>) applicationArchive;

            Set<Permission> permissions = new HashSet<>();

            if (testClass.getName().equals("org.eclipse.microprofile.jwt.tck.config.PublicKeyAsPEMLocationURLTest")
                    || testClass.getName().equals("org.eclipse.microprofile.jwt.tck.config.PublicKeyAsJWKLocationURLTest")) {
                permissions.add(new SocketPermission("localhost", "connect,resolve"));
            }

            // These classes nonsensically just call getClassLoader in System.out.printf...
            if (testClass.getName().equals("org.eclipse.microprofile.jwt.tck.container.jaxrs.ClaimValueInjectionTest")
                    || testClass.getName().equals("org.eclipse.microprofile.jwt.tck.container.jaxrs.ProviderInjectionTest")) {
                permissions.add(new RuntimePermission("getClassLoader"));
            }

            // This test class reads a temporary tck pem file
            if (testClass.getName().equals("org.eclipse.microprofile.jwt.tck.config.PublicKeyAsFileLocationURLTest")) {
                permissions.add(new FilePermission(System.getProperty("java.io.tmpdir") + File.separator + "-", "read"));
            }

            // Run the TCK with security manager
            manifestContainer.addAsManifestResource(createPermissionsXmlAsset(permissions), "permissions.xml");
        }
    }
}
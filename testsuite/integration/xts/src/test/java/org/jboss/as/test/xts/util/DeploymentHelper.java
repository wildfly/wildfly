/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.xts.util;

import java.io.File;
import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.util.PropertyPermission;

import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class DeploymentHelper {

    private static final DeploymentHelper INSTANCE = new DeploymentHelper();

    private DeploymentHelper() {

    }

    public static DeploymentHelper getInstance() {
        return INSTANCE;
    }

    public JavaArchive getJavaArchive(final String archiveName) {
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class, archiveName + ".jar")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        return javaArchive;
    }

    public WebArchive getWebArchive(final String archiveName) {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, archiveName + ".war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));

        return webArchive;
    }

    public WebArchive getWebArchiveWithPermissions(final String archiveName) {
        return ShrinkWrap.create(WebArchive.class, archiveName + ".war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                .addAsManifestResource(createPermissions(), "permissions.xml");
    }

    public static Asset createPermissions() {
        return PermissionUtils.createPermissionsXmlAsset(
                // This is technically not required given the <<ALL FILES>>. However, if that is fixed then this
                // will be required.
                new FilePermission(System.getProperties()
                        .getProperty("jbossas.ts.integ.dir") + File.separator + "xts" + File.separator
                        + "xcatalog", "read"),
                // The only reason to use <<ALL FILES>> is because the activation API cannot load the
                // implementation via a service loader without read permissions to the JAR.  (jakarta.activation.FactoryFinder)
                new FilePermission("<<ALL FILES>>", "read"),
                // Required for the org.jboss.arquillian.core.impl.RuntimeLogger
                new PropertyPermission("arquillian.debug", "read"),
                // Required for org.jboss.arquillian.container.test.spi.util.ServiceLoader
                new ReflectPermission("suppressAccessChecks"),
                // Required for org.junit.internal.MethodSorter
                new RuntimePermission("accessDeclaredMembers"),
                // Required for the activation API service loader (jakarta.activation.FactoryFinder)
                new RuntimePermission("getClassLoader"),
                // Permissions for port access
                new PropertyPermission("management.address", "read"),
                new PropertyPermission("node0", "read"),
                new PropertyPermission("jboss.http.port", "read")
        );
    }

}

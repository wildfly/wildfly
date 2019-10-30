/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.xts.util;

import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import java.io.File;
import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.net.SocketPermission;
import java.util.PropertyPermission;

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
        final String javaHome = TestSuiteEnvironment.getSystemProperty("java.home");
        final String serverHostPort = TestSuiteEnvironment.getServerAddress() + ":"
                + TestSuiteEnvironment.getHttpPort();

        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, archiveName + ".war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                        new ReflectPermission("suppressAccessChecks"),
                        new ReflectPermission("accessDeclaredMembers"),
                        // Permissions for port access
                        new PropertyPermission("management.address", "read"),
                        new PropertyPermission("node0", "read"),
                        new PropertyPermission("jboss.http.port", "read"),
                        new SocketPermission(serverHostPort, "connect,resolve"),
                        // Permissions for the new client creation
                        new RuntimePermission("accessDeclaredMembers"),
                        new RuntimePermission("createClassLoader"),
                        new RuntimePermission("getClassLoader"),
                        new RuntimePermission("org.apache.cxf.permission"),
                        new FilePermission(javaHome + File.separator + "lib" + File.separator + "wsdl.properties", "read"),
                        new PropertyPermission("user.dir", "read"),
                        new PropertyPermission("arquillian.debug", "read"),
                        new FilePermission(System.getProperty("basedir") + File.separator + "target" + File.separator
                                + "workdir" + File.separator + "xcatalog", "read")
                ), "permissions.xml");

        return webArchive;
    }

}

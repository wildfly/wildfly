/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.microprofile.faulttolerance.context.asynchronous;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FilePermission;
import java.lang.management.ManagementPermission;
import java.util.PropertyPermission;
import java.util.concurrent.ExecutionException;
import java.util.logging.LoggingPermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Adapted from Thorntail/SmallRye.
 *
 * @author Martin Kouba
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
public class AsynchronousRequestContextTestCase {

    @Deployment
    public static WebArchive createTestArchive() {
        return ShrinkWrap.create(WebArchive.class, AsynchronousRequestContextTestCase.class.getSimpleName() + ".war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackage(AsynchronousRequestContextTestCase.class.getPackage())
                .addAsManifestResource(createPermissionsXmlAsset(
                        // Permissions required by io.vertx.core / opentelemetry-sdk-extension-autoconfigure.jar
                        new FilePermission("<<ALL FILES>>", "read,write,delete"),
                        new LoggingPermission("control", null),
                        new ManagementPermission("monitor"),
                        new PropertyPermission("*", "read,write"),
                        new RuntimePermission("accessDeclaredMembers"),
                        new RuntimePermission("getClassLoader"),
                        new RuntimePermission("getStackTrace"),
                        new RuntimePermission("getenv.*"),
                        new RuntimePermission("setContextClassLoader"),
                        new RuntimePermission("shutdownHooks")
                ), "permissions.xml");
    }

    @Test
    public void testRequestContextActive(AsyncService asyncService) throws InterruptedException, ExecutionException {
        RequestFoo.DESTROYED.set(false);
        assertEquals("ok", asyncService.perform().get());
        assertTrue(RequestFoo.DESTROYED.get());
    }

}

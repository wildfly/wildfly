/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.integration.microprofile.faulttolerance.async.requestcontext;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FilePermission;
import java.util.PropertyPermission;
import java.util.concurrent.ExecutionException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Adapted from Thorntail/SmallRye.
 *
 * @author Martin Kouba
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
public class AsynchronousRequestContextTest {

    @Deployment
    public static WebArchive createTestArchive() {
        return ShrinkWrap.create(WebArchive.class, AsynchronousRequestContextTest.class.getSimpleName() + ".war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackage(AsynchronousRequestContextTest.class.getPackage())
                .addAsManifestResource(createPermissionsXmlAsset(
                        new FilePermission("<<ALL FILES>>", "read"),
                        new PropertyPermission("*", "read,write"),
                        new RuntimePermission("getenv.*"),
                        new RuntimePermission("modifyThread")
                ), "permissions.xml")
                ;
    }

    @Test
    @Ignore("Test is broken to to a dependency update. Will be fixed in WFLY-14281.")
    public void testRequestContextActive(AsyncService asyncService) throws InterruptedException, ExecutionException {
        RequestFoo.DESTROYED.set(false);
        assertEquals("ok", asyncService.perform().get());
        assertTrue(RequestFoo.DESTROYED.get());
    }

}

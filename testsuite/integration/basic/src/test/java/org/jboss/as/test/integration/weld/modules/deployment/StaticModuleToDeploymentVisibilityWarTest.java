/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.weld.modules.deployment;

import static org.jboss.as.test.integration.weld.modules.ModuleUtils.createTestModule;
import static org.jboss.as.test.integration.weld.modules.ModuleUtils.deleteRecursively;
import static org.jboss.as.test.integration.weld.modules.ModuleUtils.getModulePath;

import java.io.File;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verifies that a bean in the top-level deployment unit is visible from a static CDI-enabled module.
 *
 * @author Jozef Hartinger
 *
 */
@RunWith(Arquillian.class)
public class StaticModuleToDeploymentVisibilityWarTest {

    public static void doSetup() throws Exception {
        tearDown();
        createTestModule("foo", "foo-module.xml", ModuleBean.class, Foo.class);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        deleteRecursively(new File(getModulePath(), "test"));
    }

    @Deployment
    public static Archive<?> getDeployment() throws Exception {
        doSetup();
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(StaticModuleToDeploymentVisibilityWarTest.class, FooImpl1.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset("Dependencies: test.foo meta-inf\n"), "MANIFEST.MF");
    }

    @Inject
    private ModuleBean alpha;

    @Test
    public void testBeanAccessibility() {
        Assert.assertNotNull(alpha.getFoo());
    }
}

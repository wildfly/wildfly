/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.weld.modules.dependency;

import java.io.File;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.ModuleUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
@RunWith(Arquillian.class)
public class StaticCdiModulesDependencyTest {

    private static final String MODULE_NAME_A = "weld-module-A";
    private static final String MODULE_NAME_B = "weld-module-B";
    private static TestModule testModuleA;
    private static TestModule testModuleB;

    public static void doSetup() throws Exception {
        testModuleB = ModuleUtils.createTestModuleWithEEDependencies(MODULE_NAME_B);
        testModuleB.addResource("test-module-b.jar")
            .addClasses(ModuleBBean.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        testModuleB.create();

        // load the module.xml file and use it to create module, we need this to have Module B as exported dependency
        File moduleFile = new File(ModuleABean.class.getResource("moduleA.xml").toURI());
        testModuleA = new TestModule("test." + MODULE_NAME_A, moduleFile);
        testModuleA.addResource("test-module-a.jar")
            .addClasses(ModuleABean.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        testModuleA.create();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        testModuleA.remove();
        testModuleB.remove();
    }

    @Deployment
    public static WebArchive getDeployment() throws Exception {
        // create modules and deploy them
        doSetup();
        return ShrinkWrap.create(WebArchive.class)
            .addClasses(StaticCdiModulesDependencyTest.class, WarBean.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsManifestResource(new StringAsset("Dependencies: test." + MODULE_NAME_A + " meta-inf export\n"), "MANIFEST.MF");
    }

    @Inject
    WarBean warBean;

    @Test
    public void testBeanAccessibilities() {
        // test that WAR can use Module A bean
        Assert.assertEquals(ModuleABean.class.getSimpleName(), warBean.getModuleABean().ping());
        // verify that you can do WAR -> Module A -> Module B
        // this way we verify that module a can see and use beans from module B
        Assert.assertEquals(ModuleBBean.class.getSimpleName(), warBean.getModuleABean().pingModuleBBean());
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.packaging.multimodule;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that lifecycle methods defined on classes in a different module to the component class
 * are called.
 */
@RunWith(Arquillian.class)
public class MultiModuleLifecycleMethodTestCase {

    private static final String ARCHIVE_NAME = "MultiModuleLifecycleMethodTestCase";

    @Deployment
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");
        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClass(BaseBean.class);
        ear.addAsLibrary(lib);
        JavaArchive module = ShrinkWrap.create(JavaArchive.class, "module.jar");
        module.addClasses(MultiModuleLifecycleMethodTestCase.class, ChildBean.class);
        ear.addAsModule(module);
        return ear;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/module/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    public void testPostConstructCalled() throws Exception {
        ChildBean sfsb1 = lookup("ChildBean", ChildBean.class);
        sfsb1.doStuff();
        Assert.assertTrue(BaseBean.postConstructCalled);
    }
}

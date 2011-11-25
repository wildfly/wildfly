/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

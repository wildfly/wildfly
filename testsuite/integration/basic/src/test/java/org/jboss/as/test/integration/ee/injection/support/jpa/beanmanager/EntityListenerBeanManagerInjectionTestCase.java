/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.support.jpa.beanmanager;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class EntityListenerBeanManagerInjectionTestCase {

    private static final String ARCHIVE_NAME = "test";

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        war.addPackage(EntityListenerBeanManagerInjectionTestCase.class.getPackage());
        war.addAsWebInfResource(EntityListenerBeanManagerInjectionTestCase.class.getPackage(), "persistence.xml",
                "classes/META-INF/persistence.xml");
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @ArquillianResource
    private static InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType
                .cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    public void testBeanManagerInEntityListenerCallbacks(Foo foo, Bar bar) throws NamingException {
        TestBean bean = lookup("TestBean", TestBean.class);
        bean.createEmployee("Joe Black", "Brno 2", 20);
        bean.updateEmployee(20);
        bean.removeEmployee(20);
        // PostLoad call back is called twice
        Assert.assertEquals(8, foo.getCounter().get());
        Assert.assertEquals(8, bar.getCounter().get());
    }
}

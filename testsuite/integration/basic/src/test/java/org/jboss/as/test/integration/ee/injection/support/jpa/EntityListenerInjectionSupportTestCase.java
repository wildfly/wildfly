/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.support.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.ee.injection.support.Alpha;
import org.jboss.as.test.integration.ee.injection.support.Bravo;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Matus Abaffy
 */
@RunWith(Arquillian.class)
public class EntityListenerInjectionSupportTestCase {

    private static final String ARCHIVE_NAME = "test";

    @Deployment
    public static Archive<?> deploy() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");

        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        war.addPackage(EntityListenerInjectionSupportTestCase.class.getPackage());
        war.addClasses(Alpha.class, Bravo.class);
        war.addAsWebInfResource(EntityListenerInjectionSupportTestCase.class.getPackage(), "persistence.xml",
                "classes/META-INF/persistence.xml");
        war.addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");

        ear.addAsModule(war);
        return ear;
    }

    @ArquillianResource
    private static InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType
                .cast(iniCtx.lookup("java:app/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    public void testCDIInjectionPerformed() throws Exception {
        MyBean cmt = lookup("MyBean", MyBean.class);
        cmt.createEmployee("Joe Black", "Brno 2", 0);
        assertTrue(MyListener.isIjectionPerformed());
    }

    @Test
    public void testInterceptorCalled() throws Exception {
        MyListener.setInvocationCount(0);
        int id = 1;
        MyBean bean = lookup("MyBean", MyBean.class);
        bean.createEmployee("Joe Black", "Brno 2", id);
        assertEquals("EntityListener was not called.", 1, MyListener.getInvocationCount());
        assertTrue("Interceptor was not called.", MyListenerInterceptor.wasCalled);
        // ID should be increased by 1 in MyListenerInterceptor
        // id++;
        Employee emp = bean.getEmployeeById(id);
        assertNotNull("Could not load added employee.", emp);
    }
}

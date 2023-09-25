/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remove;

import jakarta.ejb.NoSuchEJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Scott Marlow
 * @author Jan Martiska
 * @Remove tests
 */
@RunWith(Arquillian.class)
public class RemoveTestCase {

    private static final String ARCHIVE_NAME = "RemoveTestCase";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(RemoveTestCase.class,
                SFSB1.class
        );
        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    /**
     * Ensure that invoked a bean method with the @Remove annotation, destroys the bean.
     *
     * @throws Exception
     */
    @Test
    public void testRemoveDestroysBean() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        sfsb1.done();   // first call is expected to work
        try {
            sfsb1.done();   // second call is expected to fail since we are calling a destroyed bean
            Assert.fail("Expecting NoSuchEJBException");
        } catch (NoSuchEJBException expectedException) {
            // good
        }

        Assert.assertTrue(SFSB1.preDestroyCalled);
    }

    /**
     * Ensure that a Stateful bean gets properly @Remove-d even if it throws an exception within its @PreDestroy method
     * Required by EJB 3.1 spec
     *
     * @throws Exception
     */
    @Test
    public void testRemoveDestroysBeanWhichDeniesRemoval() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        sfsb1.doneAndDenyDestruction();   // first call is expected to work
        try {
            sfsb1.doneAndDenyDestruction();   // second call is expected to fail since we are calling a destroyed bean
            Assert.fail("Expecting NoSuchEJBException");
        } catch (NoSuchEJBException expectedException) {
            // good
        }

        Assert.assertTrue(SFSB1.preDestroyCalled);
    }

}

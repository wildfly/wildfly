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

package org.jboss.as.test.integration.ejb.remove;

import javax.ejb.NoSuchEJBException;
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

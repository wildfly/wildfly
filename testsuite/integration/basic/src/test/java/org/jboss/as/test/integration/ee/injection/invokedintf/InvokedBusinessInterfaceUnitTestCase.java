/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.ee.injection.invokedintf;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test to see if the proper invoked business interface is returned. Part of migration tests from EJB Testsuite (ejbthree-1060)
 * to AS7 [JIRA JBQA-5483].
 *
 * @author Carlo de Wolf, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class InvokedBusinessInterfaceUnitTestCase {
    private static final String ARCHIVE_NAME = "business-interface-test";
    private static final Logger log = Logger.getLogger(InvokedBusinessInterfaceUnitTestCase.class);

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar").addPackage(
                InvokedBusinessInterfaceUnitTestCase.class.getPackage());
        jar.addAsManifestResource(InvokedBusinessInterfaceUnitTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    protected <Q, T> Q lookupInterface(Class<T> bean, Class<Q> intf) throws NamingException {
        log.trace("initctx: " + ctx);
        return intf.cast(ctx.lookup("java:global/" + ARCHIVE_NAME + "/" + bean.getSimpleName() + "!" + intf.getName()));
    }

    @Test
    public void testAnnotated1() throws Exception {
        Tester tester = lookupInterface(TesterBean.class, Tester.class);
        try {
            tester.testAnnotated1();
        } catch (TestFailedException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testAnnotated2() throws Exception {
        Tester tester = lookupInterface(TesterBean.class, Tester.class);
        try {
            tester.testAnnotated2();
        } catch (TestFailedException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testXml1() throws Exception {
        Tester tester = lookupInterface(TesterBean.class, Tester.class);
        try {
            tester.testXml1();
        } catch (TestFailedException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testXml2() throws Exception {
        Tester tester = lookupInterface(TesterBean.class, Tester.class);
        try {
            tester.testXml2();
        } catch (TestFailedException e) {
            Assert.fail(e.getMessage());
        }
    }
}

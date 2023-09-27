/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.packaging.war.namingcontext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Tests that EJB's packaged in a war use the correct naming context
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class WarEjbNamingContextTestCase {


    private static final String ARCHIVE_NAME = "WarEjbNamingContextTestCase";

    @Deployment
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClass(EjbInterface.class);
        ear.addAsLibrary(lib);

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "war1.war");
        war1.addClasses(WarEjbNamingContextTestCase.class, War1Ejb.class);
        ear.addAsModule(war1);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "war2.war");
        war2.addClasses(War2Ejb.class);
        ear.addAsModule(war2);
        return ear;
    }


    @Test
    public void testCorrectNamingContextUsedForEjbInWar() throws Exception {
        EjbInterface ejb = (EjbInterface) new InitialContext().lookup("java:app/war1/War1Ejb");
        Assert.assertNotNull(ejb.lookupUserTransaction());
        try {
            ejb.lookupOtherUserTransaction();
            Assert.fail();
        } catch (NamingException expected) {

        }

        ejb = (EjbInterface) new InitialContext().lookup("java:app/war2/War2Ejb");
        Assert.assertNotNull(ejb.lookupUserTransaction());
        try {
            ejb.lookupOtherUserTransaction();
            Assert.fail();
        } catch (NamingException expected) {

        }
    }


}

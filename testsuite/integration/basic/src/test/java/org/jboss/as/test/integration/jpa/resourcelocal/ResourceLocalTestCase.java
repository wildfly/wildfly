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

package org.jboss.as.test.integration.jpa.resourcelocal;

import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Transaction tests for a RESOURCE_LOCAL entity manager
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class ResourceLocalTestCase {

    private static final String ARCHIVE_NAME = "jpa_sessionfactory";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(ResourceLocalTestCase.class.getPackage());
        jar.addAsManifestResource(ResourceLocalTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        jar.addAsManifestResource(new StringAsset("Dependencies: com.h2database.h2\n"), "MANIFEST.MF");
        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    protected <T> T rawLookup(String name, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup(name));
    }

    /**
     * Even though a JTA Transaction is in progress this should throw an exception
     *
     * @throws NamingException
     */
    @Test
    public void flushOutSideTransaction() throws NamingException {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        try {
            sfsb1.flushWithNoTx();
        } catch (EJBException e) {
            Assert.assertEquals(javax.persistence.TransactionRequiredException.class, e.getCause().getClass());
        }
    }


    @Test
    public void testResourceLocalRollback() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        sfsb1.createEmployeeNoJTATransaction("Bob", "Home", 1);
        Employee emp = sfsb1.getEmployeeNoTX(1);
        Assert.assertEquals("Bob", emp.getName());
    }

}

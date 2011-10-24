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

package org.jboss.as.test.integration.ejb.remote.entity.bmp;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.Collection;
import javax.ejb.EJBMetaData;
import javax.ejb.Handle;
import javax.ejb.HomeHandle;
import javax.ejb.NoSuchEJBException;
import javax.ejb.RemoveException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests bean managed persistence
 */
@RunWith(Arquillian.class)
public class BMPRemoteEntityBeanTestCase {

    private static final String ARCHIVE_NAME = "SimpleLocalHomeTest.war";

    @ArquillianResource
    private InitialContext iniCtx;

    @Deployment
    public static Archive<?> deploy() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME);
        war.addPackage(BMPRemoteEntityBeanTestCase.class.getPackage());
        war.addAsWebInfResource("ejb/remote/entity/bmp/ejb-jar.xml", "ejb-jar.xml");
        return war;
    }

    @Test
    public void testSimpleCreate() throws Exception {
        DataStore.DATA.clear();
        final BMPHome home = getHome();
        final BMPInterface ejbInstance = home.createWithValue("Hello");
        final Integer pk = (Integer) ejbInstance.getPrimaryKey();
        Assert.assertEquals("Hello", DataStore.DATA.get(pk));
    }

    @Test
    public void testFindByPrimaryKey() throws Exception {
        DataStore.DATA.clear();
        final BMPHome home = getHome();
        DataStore.DATA.put(1099, "VALUE1099");
        BMPInterface result = home.findByPrimaryKey(1099);
        Assert.assertEquals("VALUE1099", result.getMyField());
    }

    @Test
    public void testSingleResultFinderMethod() throws Exception {
        DataStore.DATA.clear();
        final BMPHome home = getHome();
        DataStore.DATA.put(888, "VALUE888");
        BMPInterface result = home.findByValue("VALUE888");
        Assert.assertEquals("VALUE888", result.getMyField());
        Assert.assertEquals(888, result.getPrimaryKey());
    }


    @Test
    public void testCollectionFinderMethod() throws Exception {
        DataStore.DATA.clear();
        final BMPHome home = getHome();
        DataStore.DATA.put(1000, "Collection");
        DataStore.DATA.put(1001, "Collection");
        Collection<BMPInterface> col = home.findCollection();
        for (BMPInterface result : col) {
            Assert.assertEquals("Collection", result.getMyField());
        }
    }

    @Test
    public void testRemoveEntityBean() throws Exception {
        DataStore.DATA.clear();
        final BMPHome home = getHome();
        DataStore.DATA.put(56, "Remove");
        BMPInterface result = home.findByPrimaryKey(56);
        Assert.assertEquals("Remove", result.getMyField());
        result.remove();
        Assert.assertFalse(DataStore.DATA.containsKey(56));
        try {
            result.getMyField();
            fail("Expected invocation on removed instance to fail");
        } catch (NoSuchObjectException expected) {

        }
    }

    @Test
    public void testIsIdentical() throws Exception {
        DataStore.DATA.clear();
        final BMPHome home = getHome();
        DataStore.DATA.put(40, "1");
        DataStore.DATA.put(41, "2");
        BMPInterface bean1 = home.findByPrimaryKey(40);
        BMPInterface bean1_2 = home.findByPrimaryKey(40);
        BMPInterface bean2 = home.findByPrimaryKey(41);
        Assert.assertTrue(bean1.isIdentical(bean1_2));
        Assert.assertFalse(bean1.isIdentical(bean2));
    }

    @Test
    public void testEjbHomeMethod() throws Exception {
        final BMPHome home = getHome();
        Assert.assertEquals(SimpleBMPBean.HOME_METHOD_RETURN, home.exampleHomeMethod());
    }

    @Test
    public void testGetEJBLocalHome() throws Exception {
        DataStore.DATA.clear();
        final BMPHome home = getHome();
        DataStore.DATA.put(23, "23");
        BMPInterface result = home.findByPrimaryKey(23);
        final BMPHome home2 = (BMPHome) result.getEJBHome();
        Assert.assertEquals(SimpleBMPBean.HOME_METHOD_RETURN, home2.exampleHomeMethod());
    }

    @Test
    public void testHomeInterfaceEquality() throws Exception {
        final BMPHome home1 = getHome();
        final BMPHome home2 = getHome();
        Assert.assertEquals(home1, home2);
        Assert.assertEquals(home1.hashCode(), home2.hashCode());
        Assert.assertNotSame(home1, new BMPHome() {
            public BMPInterface createEmpty() {
                return null;
            }

            public BMPInterface createWithValue(String value) {
                return null;
            }

            public BMPInterface findByPrimaryKey(Integer primaryKey) {
                return null;
            }

            public BMPInterface findByValue(String value) {
                return null;
            }

            public Collection<BMPInterface> findCollection() {
                return null;
            }

            public int exampleHomeMethod() {
                return 0;
            }

            public void remove(Handle handle) throws RemoteException, RemoveException {
              
            }

            public void remove(Object primaryKey) throws RemoteException, RemoveException {
              
            }

            public EJBMetaData getEJBMetaData() throws RemoteException {
                return null;
            }

            public HomeHandle getHomeHandle() throws RemoteException {
                return null;
            }
        });
    }

    private BMPHome getHome() throws NamingException {
        return (BMPHome) iniCtx.lookup("java:global/SimpleLocalHomeTest/SimpleBMP!" + BMPHome.class.getName());
    }
}

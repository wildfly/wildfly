/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.deployment.processors;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Set;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Unit tests for {@link ViewInterfaces}.
 *
 * @author steve.coy
 *
 */
public class ViewInterfacesTestCase {

    interface InterfaceOne {
    }

    interface InterfaceTwo {
    }

    interface InterfaceThree {
    }

    static class NoSuperClassesNoInterfaces {
    }

    @Test
    public void testNoSuperClassesNoInterfaces() {
        Assert.assertEquals(0, ViewInterfaces.getPotentialViewInterfaces(NoSuperClassesNoInterfaces.class).size());
    }

    static class NoSuperClassesOneInterface implements InterfaceOne {
    }

    @Test
    public void testNoSuperClassesOneInterface() {
        Set<Class<?>> potentialViewInterfaces = ViewInterfaces.getPotentialViewInterfaces(NoSuperClassesOneInterface.class);
        Assert.assertEquals(1, potentialViewInterfaces.size());
        Assert.assertTrue(potentialViewInterfaces.contains(InterfaceOne.class));
    }

    static class NoSuperClassesTwoInterfaces implements InterfaceOne, InterfaceTwo {
    }

    @Test
    public void testNoSuperClassesTwoInterfaces() {
        Set<Class<?>> potentialViewInterfaces = ViewInterfaces
                .getPotentialViewInterfaces(NoSuperClassesTwoInterfaces.class);
        Assert.assertEquals(2, potentialViewInterfaces.size());
        Assert.assertTrue(potentialViewInterfaces.contains(InterfaceOne.class));
        Assert.assertTrue(potentialViewInterfaces.contains(InterfaceTwo.class));
    }

    @SuppressWarnings("serial")
    static class SerializableNoSuperClassesTwoInterfaces implements InterfaceOne, InterfaceTwo, Serializable {
    }

    @Test
    public void testSerializableNoSuperClassesTwoInterfaces() {
        Set<Class<?>> potentialViewInterfaces = ViewInterfaces
                .getPotentialViewInterfaces(SerializableNoSuperClassesTwoInterfaces.class);
        Assert.assertEquals(2, potentialViewInterfaces.size());
        Assert.assertTrue(potentialViewInterfaces.contains(InterfaceOne.class));
        Assert.assertTrue(potentialViewInterfaces.contains(InterfaceTwo.class));
    }

    @SuppressWarnings("serial")
    static class SessionBeanNoSuperClassesTwoInterfaces implements InterfaceOne, InterfaceTwo,
            SessionBean {

        @Override
        public void setSessionContext(SessionContext ctx) throws EJBException, RemoteException {
        }

        @Override
        public void ejbRemove() throws EJBException, RemoteException {
        }

        @Override
        public void ejbActivate() throws EJBException, RemoteException {
        }

        @Override
        public void ejbPassivate() throws EJBException, RemoteException {
        }
    }

    @Test
    public void testSessionBeanNoSuperClassesTwoInterfaces() {
        Set<Class<?>> potentialViewInterfaces = ViewInterfaces
                .getPotentialViewInterfaces(SessionBeanNoSuperClassesTwoInterfaces.class);
        Assert.assertEquals(2, potentialViewInterfaces.size());
        Assert.assertTrue(potentialViewInterfaces.contains(InterfaceOne.class));
        Assert.assertTrue(potentialViewInterfaces.contains(InterfaceTwo.class));
    }

    @SuppressWarnings("serial")
    static class SessionBeanWithMultiInterfacedSuperClass extends
            SessionBeanNoSuperClassesTwoInterfaces {
    }

    @Test
    public void testSessionBeanWithMultiInterfacedSuperClass() {
        Set<Class<?>> potentialViewInterfaces = ViewInterfaces
                .getPotentialViewInterfaces(SessionBeanWithMultiInterfacedSuperClass.class);
        Assert.assertEquals(2, potentialViewInterfaces.size());
        Assert.assertTrue(potentialViewInterfaces.contains(InterfaceOne.class));
        Assert.assertTrue(potentialViewInterfaces.contains(InterfaceTwo.class));
    }

    static class PojoWithOneInterfaceAndMultiInterfacedSuperClass extends NoSuperClassesTwoInterfaces implements InterfaceThree {
    }

    @Test
    public void testPojoWithOneInterfaceAndMultiInterfacedSuperClass() {
        Set<Class<?>> potentialViewInterfaces = ViewInterfaces
                .getPotentialViewInterfaces(PojoWithOneInterfaceAndMultiInterfacedSuperClass.class);
        Assert.assertEquals(3, potentialViewInterfaces.size());
        Assert.assertTrue(potentialViewInterfaces.contains(InterfaceOne.class));
        Assert.assertTrue(potentialViewInterfaces.contains(InterfaceTwo.class));
        Assert.assertTrue(potentialViewInterfaces.contains(InterfaceThree.class));
    }

}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.localview;

import static org.junit.Assert.fail;

import java.io.Serializable;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that local views of SLSF's are handled properly, as per EE 3.1 4.9.7
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class EjbLocalViewTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class,"testlocal.war");
        war.addPackage(EjbLocalViewTestCase.class.getPackage());
        return war;
    }

    @Test
    public void testImplicitNoInterface() throws NamingException {
        ensureExists(ImplicitNoInterfaceBean.class, ImplicitNoInterfaceBean.class, true);
        ensureDoesNotExist(ImplicitNoInterfaceBean.class, Serializable.class);
    }

    @Test
    public void testSingleImplicitInterface() throws NamingException {
        ensureExists(SimpleImplicitLocalInterfaceBean.class,ImplicitLocalInterface.class,true);
        ensureDoesNotExist(SimpleImplicitLocalInterfaceBean.class, Serializable.class);
    }

    @Test
    public void testSingleImplicitInterfaceWithSerializable() throws NamingException {
        ensureExists(ImplicitLocalInterfaceBean.class,ImplicitLocalInterface.class,true);
    }

    @Test
    public void testSingleLocalDeclaredOnBean() throws NamingException {
        ensureExists(SingleLocalDeclaredOnBean.class,OtherInterface.class,true);
        ensureDoesNotExist(SingleLocalDeclaredOnBean.class,LocalInterface.class);
    }

    @Test
    public void testSingleLocalDeclaredOnInterface() throws NamingException {
        ensureExists(SingleLocalDeclaredOnInterface.class,LocalInterface.class,true);
        ensureDoesNotExist(SingleLocalDeclaredOnInterface.class,NotViewInterface.class);
        ensureDoesNotExist(SingleLocalDeclaredOnInterface.class,Serializable.class);
    }

    @Test
    public void testTwoLocalsDeclaredOnBean() throws NamingException {
        ensureExists(TwoLocalsDeclaredOnBean.class,OtherInterface.class,false);
        ensureExists(TwoLocalsDeclaredOnBean.class,OtherInterface.class,false);
        ensureDoesNotExist(TwoLocalsDeclaredOnBean.class);
    }

    @Test
    public void testTwoLocalsDeclaredOnInterface() throws NamingException {
        ensureExists(TwoLocalsDeclaredOnInterface.class,LocalInterface.class,false);
        ensureExists(TwoLocalsDeclaredOnInterface.class,OtherLocalInterface.class,false);
        ensureDoesNotExist(TwoLocalsDeclaredOnInterface.class);
        ensureDoesNotExist(TwoLocalsDeclaredOnInterface.class,NotViewInterface.class);
        ensureDoesNotExist(TwoLocalsDeclaredOnInterface.class,Serializable.class);
    }

    private void ensureExists(Class<?> bean, Class<?> iface, boolean single) throws NamingException {
        try {
            InitialContext ctx = new InitialContext();
            ctx.lookup("java:global/testlocal/" + bean.getSimpleName() + "!" + iface.getName());
            ctx.lookup("java:app/testlocal/" + bean.getSimpleName() + "!" + iface.getName());
            ctx.lookup("java:module/" + bean.getSimpleName() + "!" + iface.getName());
            if(single) {
                ctx.lookup("java:global/testlocal/" + bean.getSimpleName());
                ctx.lookup("java:app/testlocal/" + bean.getSimpleName());
                ctx.lookup("java:module/" + bean.getSimpleName());
            }
        } catch (NameNotFoundException e) {
            fail(e.getMessage());
        }
    }

    private void ensureDoesNotExist(Class<?> bean, Class<?> iface) throws NamingException {
            lookupAndFailIfExists("java:global/testlocal/" + bean.getSimpleName() + "!" + iface.getName());
            lookupAndFailIfExists("java:app/testlocal/" + bean.getSimpleName() + "!" + iface.getName());
            lookupAndFailIfExists("java:module/" + bean.getSimpleName() + "!" + iface.getName());
    }

    private void ensureDoesNotExist(Class<?> bean) throws NamingException {
            lookupAndFailIfExists("java:global/testlocal/" + bean.getSimpleName());
            lookupAndFailIfExists("java:app/testlocal/" + bean.getSimpleName());
            lookupAndFailIfExists("java:module/" + bean.getSimpleName());
    }

    private void lookupAndFailIfExists(String name) throws NamingException {
        try {
            InitialContext ctx = new InitialContext();
            ctx.lookup(name);
            fail("Entry " + name + " was bound to JNDI when it should not be");
        } catch (NameNotFoundException e) {

        }
    }

}

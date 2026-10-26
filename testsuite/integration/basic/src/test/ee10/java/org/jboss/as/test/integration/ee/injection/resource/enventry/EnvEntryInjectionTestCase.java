/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.enventry;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

/**
 * A test for managed bean injection via env-entry in web.xml
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class EnvEntryInjectionTestCase {

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "war-example.war");
        war.addClasses(
                EnvEntryManagedBean.class,
                EnvEntryInjectionTestCase.class
        );
        war.addAsWebInfResource(EnvEntryInjectionTestCase.class.getPackage(), "EnvEntryInjectionTestCase-web.xml", "web.xml");
        return war;
    }


    @Test
    public void testEnvEntryInjectionIntoManagedBean() throws NamingException {
        final InitialContext initialContext = new InitialContext();
        final EnvEntryManagedBean bean = (EnvEntryManagedBean) initialContext.lookup("java:module/" + EnvEntryManagedBean.class.getName());
        Assertions.assertEquals("bye", bean.getExistingString());
    }

    @Test
    public void testEnvEntryNonExistentManagedBean() throws NamingException {
        final InitialContext initialContext = new InitialContext();
        final EnvEntryManagedBean bean = (EnvEntryManagedBean) initialContext.lookup("java:module/" + EnvEntryManagedBean.class.getName());
        Assertions.assertEquals("hi", bean.getNonExistentString());
    }

    @Test
    public void testNonExistentResourceCannotBeLookedUp() throws NamingException {
        try {
            final InitialContext initialContext = new InitialContext();
            initialContext.lookup("java:module/env/" + EnvEntryManagedBean.class.getName() + "/nonExistentString");
            Assertions.fail();
        } catch (NamingException expected) {

        }
    }

    @Test
    public void testBoxedUnboxedMismatch() throws NamingException {
        final InitialContext initialContext = new InitialContext();
        final EnvEntryManagedBean bean = (EnvEntryManagedBean) initialContext.lookup("java:module/" + EnvEntryManagedBean.class.getName());
        Assertions.assertEquals(10, bean.getByteField());
    }
}

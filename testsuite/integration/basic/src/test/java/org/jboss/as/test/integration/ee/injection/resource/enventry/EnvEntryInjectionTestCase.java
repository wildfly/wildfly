/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ee.injection.resource.enventry;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A test for injection via env-entry in web.xml
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class EnvEntryInjectionTestCase {

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "war-example.war");
        war.addClasses(
                EnvEntryInjectionServlet.class,
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
        Assert.assertEquals("bye", bean.getExistingString());
    }

    @Test
    public void testEnvEntryNonExistentManagedBean() throws NamingException {
        final InitialContext initialContext = new InitialContext();
        final EnvEntryManagedBean bean = (EnvEntryManagedBean) initialContext.lookup("java:module/" + EnvEntryManagedBean.class.getName());
        Assert.assertEquals("hi", bean.getNonExistentString());
    }

    @Test
    public void testNonExistentResourceCannotBeLookedUp() throws NamingException {
        try {
            final InitialContext initialContext = new InitialContext();
            initialContext.lookup("java:module/env/" + EnvEntryManagedBean.class.getName() + "/nonExistentString");
            Assert.fail();
        } catch (NamingException expected) {

        }
    }

    @Test
    public void testBoxedUnboxedMismatch() throws NamingException {
        final InitialContext initialContext = new InitialContext();
        final EnvEntryManagedBean bean = (EnvEntryManagedBean) initialContext.lookup("java:module/" + EnvEntryManagedBean.class.getName());
        Assert.assertEquals(10, bean.getByteField());
    }
}

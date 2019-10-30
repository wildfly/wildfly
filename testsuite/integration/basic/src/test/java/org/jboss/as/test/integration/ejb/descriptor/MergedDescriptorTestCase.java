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
package org.jboss.as.test.integration.ejb.descriptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
public class MergedDescriptorTestCase extends AbstractCustomDescriptorTests {
    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb-descriptor-test.jar")
            .addPackage(DescriptorGreeterBean.class.getPackage())
            .addAsManifestResource(MergedDescriptorTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml")
            .addAsManifestResource(MergedDescriptorTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    @ArquillianResource
    private InitialContext ctx;

    @Test
    public void testAnnotated() throws NamingException {
        try {
            final AnnotatedGreeterBean bean = (AnnotatedGreeterBean) ctx.lookup("java:global/ejb-descriptor-test/AnnotatedGreeter!org.jboss.as.test.integration.ejb.descriptor.AnnotatedGreeterBean");
            final String name = "testAnnotated";
            final String result = bean.greet(name);
            assertEquals("Hi testAnnotated", result);
        } catch (NameNotFoundException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testSpec() throws NamingException {
        try {
            final DescriptorGreeterBean bean = (DescriptorGreeterBean) ctx.lookup("java:global/ejb-descriptor-test/SpecGreeter");
            final String name = "testSpec";
            final String result = bean.greet(name);
            assertEquals("Hi testSpec", result);
        } catch (NameNotFoundException e) {
            fail(e.getMessage());
        }
    }

    /**
     * AS7-2634 The @Singleton annotation should be used to determine that the bean definied in ejb-jar.xml is a singleton
     * bean.
     */
    @Test
    public void testSessionTypeCanBeDeterminedFromAnnotation() throws NamingException {
        try {
            SessionTypeSpecifiedBean bean = (SessionTypeSpecifiedBean) ctx.lookup("java:module/SessionTypeSpecifiedBean");
            assertEquals(0, bean.increment());
            assertEquals(1, bean.increment());
            bean = (SessionTypeSpecifiedBean) ctx.lookup("java:module/RedefinedSingletonBean");
            assertEquals(0, bean.increment());
            assertEquals(1, bean.increment());
        } catch (NameNotFoundException e) {
            fail(e.getMessage());
        }
    }
}

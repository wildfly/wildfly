/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

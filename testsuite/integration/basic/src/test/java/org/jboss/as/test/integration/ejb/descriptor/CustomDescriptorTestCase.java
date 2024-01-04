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
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
public class CustomDescriptorTestCase extends AbstractCustomDescriptorTests {
    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb-descriptor-test.jar")
            .addPackage(DescriptorGreeterBean.class.getPackage())
            .addAsManifestResource(CustomDescriptorTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return jar;
    }

//    @EJB
//    private DescriptorGreeterBean bean;

    @Test
    public void testAnnotated() throws NamingException {
        final InitialContext ctx = new InitialContext();
        try {
            final AnnotatedGreeterBean bean = (AnnotatedGreeterBean) ctx.lookup("java:global/ejb-descriptor-test/AnnotatedGreeter!org.jboss.as.test.integration.ejb.descriptor.AnnotatedGreeterBean");
            final String name = "testAnnotated";
            final String result = bean.greet(name);
            assertEquals("Hi testAnnotated", result);
        } catch (NameNotFoundException e) {
            fail(e.getMessage());
        }
    }
}

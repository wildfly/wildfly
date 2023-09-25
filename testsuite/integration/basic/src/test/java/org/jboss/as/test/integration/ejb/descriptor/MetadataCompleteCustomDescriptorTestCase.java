/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.descriptor;

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
public class MetadataCompleteCustomDescriptorTestCase extends AbstractCustomDescriptorTests {
    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb-descriptor-test.jar")
                .addPackage(DescriptorGreeterBean.class.getPackage())
                .addAsManifestResource(MetadataCompleteCustomDescriptorTestCase.class.getPackage(), "jboss-ejb3-md-complete.xml", "jboss-ejb3.xml");
        return jar;
    }

    @Test
    public void testAnnotated() throws NamingException {
        final InitialContext ctx = new InitialContext();
        try {
            final AnnotatedGreeterBean bean = (AnnotatedGreeterBean) ctx.lookup("java:global/ejb-descriptor-test/AnnotatedGreeter!org.jboss.as.test.integration.ejb.descriptor.AnnotatedGreeterBean");
            fail("The annotated bean should not be available");
        } catch (NameNotFoundException e) {
            // good
        }
    }
}

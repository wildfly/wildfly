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

import org.junit.Test;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class AbstractCustomDescriptorTests {
    @Test
    public void test1() throws NamingException {
        final InitialContext ctx = new InitialContext();
        try {
            final DescriptorGreeterBean bean = (DescriptorGreeterBean) ctx.lookup("java:global/ejb-descriptor-test/DescriptorGreeter!org.jboss.as.test.integration.ejb.descriptor.DescriptorGreeterBean");
            final String name = "test1";
            final String result = bean.greet(name);
            assertEquals("Hi test1", result);
        } catch (NameNotFoundException e) {
            fail(e.getMessage());
        }
    }
}

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

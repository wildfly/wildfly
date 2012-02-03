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

package org.jboss.as.test.integration.ejb.remote.distinctname;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;

/**
 * Tests that invocation on EJBs deployed in a deployment with distinct name works successfully
 *
 * @author Jaikiran Pai
 */
public abstract class DistinctNameTestCase {

    private static Context context;

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Hashtable props = new Hashtable();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        context = new InitialContext(props);
    }

    /**
     * Test that invocation on a stateless bean, deployed in a deployment with distinct-name, works fine
     *
     * @throws Exception
     */
    @Test
    public void testRemoteSLSBInvocation() throws Exception {
        final Echo bean = (Echo) context.lookup("ejb:" + getAppName() + "/" + getModuleName() + "/" + getDistinctName()
                + "/" + StatelessEcho.class.getSimpleName() + "!" + Echo.class.getName());
        Assert.assertNotNull("Lookup returned a null bean proxy", bean);
        final String msg = "Hello world from a really remote client!!!";
        final String echo = bean.echo(msg);
        Assert.assertEquals("Unexpected echo returned from remote stateless bean", msg, echo);
    }

    /**
     * Test that invocation on a stateful bean, deployed in a deployment with distinct-name, works fine
     *
     * @throws Exception
     */
    @Test
    public void testRemoteSFSBInvocation() throws Exception {
        final Echo bean = (Echo) context.lookup("ejb:" + getAppName() + "/" + getModuleName() + "/" + getDistinctName()
                + "/" + StatefulEcho.class.getSimpleName() + "!" + Echo.class.getName() + "?stateful");
        Assert.assertNotNull("Lookup returned a null bean proxy", bean);
        final String msg = "Hello world from a really remote client!!!";
        final String echo = bean.echo(msg);
        Assert.assertEquals("Unexpected echo returned from remote stateful bean", msg, echo);

    }

    /**
     * Test that invocation on a singleton bean, deployed in a deployment with distinct-name, works fine
     *
     * @throws Exception
     */
    @Test
    public void testRemoteSingletonInvocation() throws Exception {
        final Echo bean = (Echo) context.lookup("ejb:" + getAppName() + "/" + getModuleName() + "/" + getDistinctName()
                + "/" + SingletonEcho.class.getSimpleName() + "!" + Echo.class.getName());
        Assert.assertNotNull("Lookup returned a null bean proxy", bean);
        final String msg = "Hello world from a really remote client!!!";
        final String echo = bean.echo(msg);
        Assert.assertEquals("Unexpected echo returned from remote singleton bean", msg, echo);

    }

    protected abstract String getAppName();

    protected abstract String getModuleName();

    protected abstract String getDistinctName();
}

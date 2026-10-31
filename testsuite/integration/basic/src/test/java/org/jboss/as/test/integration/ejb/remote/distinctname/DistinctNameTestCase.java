/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.distinctname;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

    @BeforeAll
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
        Assertions.assertNotNull(bean, "Lookup returned a null bean proxy");
        final String msg = "Hello world from a really remote client!!!";
        final String echo = bean.echo(msg);
        Assertions.assertEquals(msg, echo, "Unexpected echo returned from remote stateless bean");
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
        Assertions.assertNotNull(bean, "Lookup returned a null bean proxy");
        final String msg = "Hello world from a really remote client!!!";
        final String echo = bean.echo(msg);
        Assertions.assertEquals(msg, echo, "Unexpected echo returned from remote stateful bean");

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
        Assertions.assertNotNull(bean, "Lookup returned a null bean proxy");
        final String msg = "Hello world from a really remote client!!!";
        final String echo = bean.echo(msg);
        Assertions.assertEquals(msg, echo, "Unexpected echo returned from remote singleton bean");

    }

    protected abstract String getAppName();

    protected abstract String getModuleName();

    protected abstract String getDistinctName();
}

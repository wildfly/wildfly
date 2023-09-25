/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.descriptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.common.Naming;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * [AS7-835] ejb-jar.xml doesn't need to define a session-type
 *
 * https://issues.jboss.org/browse/AS7-835
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
public class AS835TestCase {

    @Deployment
    public static Archive<?> deployment() {
        // using JavaArchive doesn't work, because of a bug in Arquillian, it only deploys wars properly
        WebArchive deployment = ShrinkWrap.create(WebArchive.class, "as835.war")
                .addPackage(SimpleStatelessBean.class.getPackage())
                .addPackage(Naming.class.getPackage())
                .addAsWebInfResource(AS835TestCase.class.getPackage(), "ejb-jar-as835.xml", "ejb-jar.xml");
        return deployment;
    }

    /**
     * Make sure the ejb-jar.xml is actually processed.
     */
    @Test
    public void testEnvEntry() throws NamingException {
        final SimpleStatelessBean bean = Naming.lookup("java:global/as835/SimpleStatelessBean", SimpleStatelessBean.class);
        final String envValue = bean.getTest();
        // see ejb-jar.xml for the value
        assertEquals("*Hello world", envValue);
    }

    /**
     * Make sure the ejb-jar.xml is actually processed.
     */
    @Test
    public void testInterceptor() throws NamingException {
        final SimpleStatelessBean bean = Naming.lookup("java:global/as835/SimpleStatelessBean", SimpleStatelessBean.class);
        final String envValue = bean.getTest();
        // see SimpleStatelessBean.aroundInvoke for the value
        assertNotNull(envValue);
        assertTrue(envValue.startsWith("*"));
    }

    @Test
    public void testInvocation() throws NamingException {
        final SimpleStatelessBean bean = Naming.lookup("java:global/as835/SimpleStatelessBean", SimpleStatelessBean.class);
        bean.getTest();
        // if we can invoke the bean it must have been deployed properly
    }

}

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
package org.jboss.as.test.integration.internals.as7_835;

import static org.jboss.as.test.integration.common.Naming.lookup;
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
public class DeployUnknownSessionTypeTestCase {
    @Deployment
    public static Archive<?> deployment() {
        // using JavaArchive doesn't work, because of a bug in Arquillian, it only deploys wars properly
        WebArchive deployment = ShrinkWrap.create(WebArchive.class, "as7_835.war")
                .addPackage(SimpleStatelessBean.class.getPackage())
                .addPackage(Naming.class.getPackage())
                .addAsWebInfResource("as7_835.jar/META-INF/ejb-jar.xml", "ejb-jar.xml");
        System.out.println(deployment.toString(true));
        return deployment;
    }

    /**
     * Make sure the ejb-jar.xml is actually processed.
     */
    @Ignore("@Resource java.lang.String var; isn't working")
    @Test
    public void testEnvEntry() throws NamingException {
        final SimpleStatelessBean bean = lookup("java:global/as7_835/SimpleStatelessBean", SimpleStatelessBean.class);
        final String envValue = bean.getTest();
        // see ejb-jar.xml for the value
        assertEquals("*Hello world", envValue);
    }

    /**
     * Make sure the ejb-jar.xml is actually processed.
     */
    @Ignore("Interceptor overrides from xml are not working")
    @Test
    public void testInterceptor() throws NamingException {
        final SimpleStatelessBean bean = lookup("java:global/as7_835/SimpleStatelessBean", SimpleStatelessBean.class);
        final String envValue = bean.getTest();
        // see SimpleStatelessBean.aroundInvoke for the value
        assertNotNull(envValue);
        assertTrue(envValue.startsWith("*"));
    }

    @Test
    public void testInvocation() throws NamingException {
        final SimpleStatelessBean bean = lookup("java:global/as7_835/SimpleStatelessBean", SimpleStatelessBean.class);
        bean.getTest();
        // if we can invoke the bean it must have been deployed properly
    }
}

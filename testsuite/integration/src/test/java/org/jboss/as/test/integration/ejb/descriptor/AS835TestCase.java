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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.common.Naming;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
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
                .addAsWebInfResource(new StringAsset(EJB_JAR), "ejb-jar.xml");
        System.out.println(deployment.toString(true));
        return deployment;
    }

    /**
     * Make sure the ejb-jar.xml is actually processed.
     */
    @Ignore("@Resource java.lang.String var; isn't working")
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
    @Ignore("Interceptor overrides from xml are not working")
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

    private static final String EJB_JAR="<ejb-jar xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_1.xsd\"\n" +
            "         version=\"3.1\">\n" +
            "    <enterprise-beans>\n" +
            "        <session>\n" +
            "            <ejb-name>SimpleStatelessBean</ejb-name>\n" +
            "            <!-- AS7-835: do not specify ejb-class and session-type\n" +
            "            <ejb-class>org.jboss.as.test.integration.internals.as7_835.SimpleStatelessBean</ejb-class>\n" +
            "            <session-type>Stateless</session-type>\n" +
            "            -->\n" +
            "            <env-entry>\n" +
            "                <env-entry-name>test</env-entry-name>\n" +
            "                <env-entry-type>java.lang.String</env-entry-type>\n" +
            "                <env-entry-value>Hello world</env-entry-value>\n" +
            "            </env-entry>\n" +
            "            <around-invoke>\n" +
            "                <method-name>aroundInvoke</method-name>\n" +
            "            </around-invoke>\n" +
            "        </session>\n" +
            "    </enterprise-beans>\n" +
            "</ejb-jar>";
}

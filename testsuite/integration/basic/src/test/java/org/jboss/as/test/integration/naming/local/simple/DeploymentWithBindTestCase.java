/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.naming.local.simple;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertNotNull;

/**
 * @author John Bailey
 */
@RunWith(Arquillian.class)
public class DeploymentWithBindTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war");
        war.addClasses(HttpRequest.class, BeanWithBind.class, ServletWithBind.class);
        return war;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    @ArquillianResource
    private ManagementClient managementClient;

    protected <T> T lookup(Class<T> beanType) throws NamingException {
        return beanType.cast(iniCtx.lookup("java:global/test/" + beanType.getSimpleName() + "!" + beanType.getName()));
    }

    @Test
    public void testEjb() throws Exception {
        final BeanWithBind bean = lookup(BeanWithBind.class);
        bean.doBind();
        assertNotNull(iniCtx.lookup("java:jboss/test"));
        assertNotNull(iniCtx.lookup("java:/test"));
        bean.checkBind();
    }

    private String performCall(String urlPattern, String op) throws Exception {
        return HttpRequest.get("http://" + managementClient.getMgmtAddress() + ":8080/test/" + urlPattern + "?op=" + op, 10, SECONDS);
    }

    @Test
    public void testServlet() throws Exception {
        performCall("simple", "bind");
        assertNotNull(iniCtx.lookup("java:jboss/test"));
        assertNotNull(iniCtx.lookup("java:/test"));
        performCall("simple", "check");
    }
}

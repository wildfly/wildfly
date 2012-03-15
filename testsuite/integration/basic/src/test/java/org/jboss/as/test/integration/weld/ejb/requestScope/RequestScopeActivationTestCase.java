/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.weld.ejb.requestScope;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * AS7-4119
 * <p/>
 * Tests that request scope is active in in-vm remote interface requests
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RequestScopeActivationTestCase {

    @ArquillianResource
    @OperateOnDeployment("web")
    private URL web;

    @ArquillianResource
    @OperateOnDeployment("web2")
    private URL web2;

    @Deployment(name = "ejb")
    public static Archive<?> ejb() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        jar.addClasses(RemoteEjb.class, RemoteInterface.class, RequestScopedBean.class);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return jar;
    }

    @Deployment(name = "web")
    public static Archive<?> web() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "web.war");
        war.addClasses(RequestServlet.class, RemoteInterface.class);
        return war;
    }

    @Deployment(name = "web2")
    public static Archive<?> web2() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "web2.war");
        war.addClasses(RequestServlet.class, RemoteInterface.class);
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Test
    public void testRequestContextActiveNoCDI() throws IOException, ExecutionException, TimeoutException {
        String response = HttpRequest.get(web.toExternalForm(), 10, TimeUnit.SECONDS);
        assertEquals(RequestScopedBean.MESSAGE, response);
    }
    @Test
    public void testRequestContextActiveCDI() throws IOException, ExecutionException, TimeoutException {
        String response = HttpRequest.get(web2.toExternalForm(), 10, TimeUnit.SECONDS);
        assertEquals(RequestScopedBean.MESSAGE, response);
    }
}

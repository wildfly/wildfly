/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.web.filter;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

/**
 * BZ-1235627
 *
 * @author Tomas Hofman (thofman@redhat.com)
 */
@RunWith(Arquillian.class)
@RunAsClient
public class AnnotatedFilterDeploymentTestCase {

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = "v30")
    public static WebArchive deployV30() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "v30.war");
        war.addClasses(AnnotatedFilter.class, EmptyServlet.class);
        war.addAsWebInfResource(AnnotatedFilterDeploymentTestCase.class.getPackage(), "web30.xml", "web.xml");
        return war;
    }

    @Deployment(name = "v30MetadataComplete")
    public static WebArchive deployV30MetadataComplete() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "v30MetadataComplete.war");
        war.addClasses(AnnotatedFilter.class, EmptyServlet.class);
        war.addAsWebInfResource(AnnotatedFilterDeploymentTestCase.class.getPackage(), "web30_metadata_complete.xml", "web.xml");
        return war;
    }

    @Deployment(name = "v24")
    public static WebArchive deployV24() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "v24.war");
        war.addClasses(AnnotatedFilter.class, EmptyServlet.class);
        war.addAsWebInfResource(AnnotatedFilterDeploymentTestCase.class.getPackage(), "web24.xml", "web.xml");
        return war;
    }

    private String performCall(URL url, String urlPattern) throws Exception {
        String finalUrl = url.toURI().resolve(urlPattern).toString();
        return HttpRequest.get(finalUrl, 1, SECONDS);
    }

    @Test
    @OperateOnDeployment("v24")
    public void testFilterPresent24(@ArquillianResource URL url) throws Exception {
        String result = performCall(url, "EmptyServlet");
        assertEquals(AnnotatedFilter.OUTPUT, result);
    }

    @Test
    @OperateOnDeployment("v30")
    public void testFilterPresent30(@ArquillianResource URL url) throws Exception {
        String result = performCall(url, "EmptyServlet");
        assertEquals(AnnotatedFilter.OUTPUT, result);
    }

    @Test
    @OperateOnDeployment("v30MetadataComplete")
    public void testFilterPresent30MetadataComplete(@ArquillianResource URL url) throws Exception {
        String result = performCall(url, "EmptyServlet");
        assertEquals(EmptyServlet.OUTPUT, result);
    }
}

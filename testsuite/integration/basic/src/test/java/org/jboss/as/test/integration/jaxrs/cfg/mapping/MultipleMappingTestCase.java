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
package org.jboss.as.test.integration.jaxrs.cfg.mapping;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.jaxrs.packaging.war.WebXml;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for RESTEasy multiple mapping of servlet of JAX-RS impl.
 * 
 * @author Pavel Janousek
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MultipleMappingTestCase {
    private static final String depMultipleUrl = "mapping_multiple_url";
    private static final String depMultipleMapping = "mapping_multiple_mapping";

    @Deployment(name=depMultipleUrl)
    public static Archive<?> deploy_multiple_url() {
        return ShrinkWrap
                .create(WebArchive.class, depMultipleUrl + ".war")
                .addClasses(MultipleMappingTestCase.class, HelloWorldResource.class)
                .setWebXML(
                        WebXml.get("<servlet-mapping>\n"
                                + "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n"
                                + "        <url-pattern>/map1/*</url-pattern>\n" 
                                + "        <url-pattern>/map2/*</url-pattern>\n" 
                                + "</servlet-mapping>\n"));
    }

    @Deployment(name=depMultipleMapping)
    public static Archive<?> deploy_multiple_mapping() {
        return ShrinkWrap
                .create(WebArchive.class, depMultipleMapping + ".war")
                .addClasses(MultipleMappingTestCase.class, HelloWorldResource.class)
                .setWebXML(
                        WebXml.get("<servlet-mapping>\n"
                                + "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n"
                                + "        <url-pattern>/map1/*</url-pattern>\n" 
                                + "</servlet-mapping>\n"
                                + "<servlet-mapping>\n"
                                + "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n"
                                + "        <url-pattern>/map2/*</url-pattern>\n" 
                                + "</servlet-mapping>\n"));
    }

    @Ignore("JBPAPP-8603")
    @Test
    @OperateOnDeployment(depMultipleUrl)
    public void testMultipleUrlInMapping(@ArquillianResource URL url) throws Exception {
        final String res_string = "Hello World!";

        String result = HttpRequest.get(url.toExternalForm() + "map1/helloworld", 10, TimeUnit.SECONDS);
        assertEquals(res_string, result);

        result = HttpRequest.get(url.toExternalForm() + "map2/helloworld", 10, TimeUnit.SECONDS);
        assertEquals(res_string, result);
    }

    @Ignore("JBPAPP-8603")
    @Test
    @OperateOnDeployment(depMultipleMapping)
    public void testMultipleMapping(@ArquillianResource URL url) throws Exception {
        final String res_string = "Hello World!";

        String result = HttpRequest.get(url.toExternalForm() + "map1/helloworld", 10, TimeUnit.SECONDS);
        assertEquals(res_string, result);

        result = HttpRequest.get(url.toExternalForm() + "map2/helloworld", 10, TimeUnit.SECONDS);
        assertEquals(res_string, result);
    }
}

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2020, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.test.integration.ejb.validation;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URL;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 * */
@RunWith(Arquillian.class)
@RunAsClient
public class EjbBeanValidationTestCase {

    @ApplicationPath("")
    public static class TestApplication extends Application {
    }

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "ejbvalidation.war")
                .addPackage(HttpRequest.class.getPackage())
                .addClasses(
                        EjbBeanValidationTestCase.class,
                        TestApplication.class,
                        TestResource.class
                );
    }

    @ArquillianResource
    private URL url;

    static Client client;

    @BeforeClass
    public static void setUpClient() {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void close() {
        client.close();
    }


    @Test
    public void testValidation() {
        Response response = client.target(url.toExternalForm() + "validate/1").request().get();
        Assert.assertNotNull("Request should have been negatively validated", response.getMetadata().getFirst("validation-exception"));
    }

    /**
     * This test check whether the EJB proxy is being correctly normalized by {@link org.jboss.as.ejb3.validator.EjbProxyBeanMetaDataClassNormalizer}.
     * As the proxy does not support contain information about generics, without normalization validation would fail.
     */
    @Test
    public void testProxyNormalization() {
        String result = client.target(url.toExternalForm() + "put/list")
                .request(MediaType.APPLICATION_JSON).post(Entity.json("[\"a\",\"b\",\"c\"]"), String.class);
        Assert.assertEquals("a, b, c", result);
    }

}

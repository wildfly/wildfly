/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.test.integration.jaxrs.validator;

import java.net.URL;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for JAX-RS taking the global Bean Validation configuration into account (META-INF/validation.xml).
 *
 * @author Gunnar Morling
 */
@RunWith(Arquillian.class)
@RunAsClient
public class BeanValidationConfiguredGloballyTestCase {

    @ApplicationPath("/myjaxrs")
    public static class TestApplication extends Application {
    }

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "jaxrsnoap.war")
            .addPackage(HttpRequest.class.getPackage())
            .addClasses(
                BeanValidationConfiguredGloballyTestCase.class,
                ValidatorModel.class,
                GloballyConfiguredValidatorResource.class
            )
            .addAsManifestResource(new StringAsset(
                "<validation-config version=\"1.1\" xmlns=\"http://jboss.org/xml/ns/javax/validation/configuration\">\n" +
                    "<executable-validation>\n" +
                        "<default-validated-executable-types>\n" +
                            "<executable-type>NONE</executable-type>\n" +
                        "</default-validated-executable-types>\n" +
                    "</executable-validation>\n" +
                "</validation-config>"), "validation.xml"
            );
    }

    @ArquillianResource
    private URL url;

    @Test
    public void testInvalidRequestsAreAcceptedDependingOnGlobalValidationConfiguration() throws Exception {
        DefaultHttpClient client = new DefaultHttpClient(new PoolingClientConnectionManager());

        HttpGet get = new HttpGet(url + "myjaxrs/globally-configured-validate/3/disabled");
        HttpResponse result = client.execute(get);

        Assert.assertEquals("No constraint violated", 200, result.getStatusLine().getStatusCode());
        EntityUtils.consume(result.getEntity());

        get = new HttpGet(url + "myjaxrs/globally-configured-validate/3/enabled");
        result = client.execute(get);
        Assert.assertEquals("Parameter constraint violated", 400, result.getStatusLine().getStatusCode());
    }
}

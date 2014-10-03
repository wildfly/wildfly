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
package org.jboss.as.test.integration.web.jsp.taglib.external;

import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.shared.ModuleUtils;
import org.jboss.as.test.shared.TempTestModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(Arquillian.class)
@RunAsClient
public class ExternalTagLibTestCase {

    private static final String MODULE_NAME = "external-tag-lib";
    private static TempTestModule testModule;
    public static void doSetup() throws Exception {
        testModule = ModuleUtils.createTestModuleWithEEDependencies(MODULE_NAME);
        JavaArchive jar = testModule.addResource("module.jar");
        jar.addClass(ExternalTag.class);
        jar.addAsManifestResource(ExternalTagLibTestCase.class.getPackage(), "external.tld", "external.tld");
        testModule.create(true);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        testModule.remove();
    }


    @Deployment
    public static WebArchive deployment() throws Exception {
        doSetup();
        return ShrinkWrap.create(WebArchive.class, "test-external.war")
                .addClass(InternalTag.class)
                .addAsWebInfResource(ExternalTagLibTestCase.class.getPackage(), "internal.tld", "internal.tld")
                .addAsManifestResource(new StringAsset("Dependencies: test." + MODULE_NAME + " meta-inf\n"), "MANIFEST.MF")
                .addAsWebResource(ExternalTagLibTestCase.class.getPackage(), "test.jsp", "test.jsp");
    }

    @ArquillianResource
    private URL url;

    @Test
    public void testExternalTagLib() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        try {

            HttpGet httpget = new HttpGet(url.toExternalForm() + "/test.jsp");
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity);
            Assert.assertTrue(result, result.contains("External Tag!"));
            Assert.assertTrue(result, result.contains("Internal Tag!"));
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }
    }

}
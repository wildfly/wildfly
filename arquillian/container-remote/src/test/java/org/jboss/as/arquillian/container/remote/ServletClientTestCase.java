/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.container.remote;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.remote.servlet.Servlet1;
import org.jboss.as.arquillian.container.remote.servlet.Servlet2;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test to verify correct Protocol Metadata returned from Deployment
 * 
 * @author <a href="aslak@redhat.com">Aslak Knutsen</a>
 */
@RunWith(Arquillian.class)
public class ServletClientTestCase {

    @Deployment(name = "war", testable = false)
    public static WebArchive createWarDeployment() throws Exception {
        return ShrinkWrap.create(WebArchive.class).addClass(Servlet1.class);
    }

    @Deployment(name = "ear", testable = false)
    public static EnterpriseArchive createEarDeployment() throws Exception {
        return ShrinkWrap
                .create(EnterpriseArchive.class)
                .addAsModule(
                        ShrinkWrap.create(WebArchive.class).addClass(
                                Servlet1.class))
                .addAsModule(
                        ShrinkWrap.create(WebArchive.class).addClass(
                                Servlet2.class));
    }
    
    @Deployment(name = "war-no-servlet", testable = false)
    public static WebArchive createNoContextWebDeployment() 
    {
        return ShrinkWrap.create(WebArchive.class)
                .addAsWebResource(new StringAsset("JSP"), "index.jsp");
    }

    @Test @OperateOnDeployment("war")
    public void shouldBeAbleToLookupServletURLInAWar(@ArquillianResource URL baseURL) throws Exception {
        Assert.assertNotNull("Should have injected Base URL for deployed WebContext",
                baseURL);
        
        Assert.assertEquals(Servlet1.class.getName(), getContent(new URL(baseURL, Servlet1.PATTERN)));
    }

    @Test @OperateOnDeployment("ear")
    public void shouldBeAbleToLookupServletURLInAEar(
            @ArquillianResource(Servlet1.class) URL servlet1BaseURL, 
            @ArquillianResource(Servlet2.class) URL servlet2BaseURL) throws Exception {
        
        Assert.assertNotNull("Should have injected Base URL for deployed WebContext",
                servlet1BaseURL);
        Assert.assertEquals(Servlet1.class.getName(), getContent(new URL(servlet1BaseURL, Servlet1.PATTERN)));

        Assert.assertNotNull("Should have injected Base URL for deployed WebContext",
                servlet2BaseURL);
        Assert.assertEquals(Servlet2.class.getName(), getContent(new URL(servlet2BaseURL, Servlet2.PATTERN)));
    }

    @Test @OperateOnDeployment("war-no-servlet")
    public void shouldBeAbleToDeployWarWithNoServlets(@ArquillianResource URL baseURL) throws Exception
    {
        Assert.assertNotNull("Should have injected Base URL for deployed WebContext",
                baseURL);

        Assert.assertEquals("JSP", getContent(new URL(baseURL, "index.jsp")));
    }
    
    private String getContent(URL url) throws Exception {
        InputStream is = url.openStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            int read;
            while ((read = is.read()) != -1) {
                out.write(read);
            }
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
        }
        return out.toString();
    }
}

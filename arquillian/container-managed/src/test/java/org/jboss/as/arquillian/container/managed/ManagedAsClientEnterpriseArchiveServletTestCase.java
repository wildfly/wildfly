/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.arquillian.container.managed;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A client-side test case that verifies that the root deployment URL can be injected
 * into the test case.
 *
 * @author <a href="http://community.jboss.org/people/dan.j.allen">Dan Allen</a>
 */
@RunWith(Arquillian.class)
public class ManagedAsClientEnterpriseArchiveServletTestCase {

    @Deployment(testable = false)
    public static EnterpriseArchive createDeployment() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class).addClass(HelloWorldServlet.class);
        return ShrinkWrap.create(EnterpriseArchive.class).addAsModule(war);
    }

    @ArquillianResource
    private URL deploymentUrl;

    @Test
    public void shouldBeAbleToInvokeServlet() throws Exception {
        Assert.assertNotNull(deploymentUrl);
        String result = getContent(new URL(deploymentUrl.toString() + HelloWorldServlet.URL_PATTERN.substring(1)));
        Assert.assertEquals(HelloWorldServlet.GREETING, result);
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

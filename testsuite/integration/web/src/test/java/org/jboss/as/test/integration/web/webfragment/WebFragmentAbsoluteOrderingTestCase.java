/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.webfragment;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test absolute ordering works even if some web fragments are missing
 * <p>
 * see https://issues.jboss.org/browse/WFLY-6552
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WebFragmentAbsoluteOrderingTestCase {

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive single() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "single.war");
        war.addAsWebInfResource(WebFragmentAbsoluteOrderingTestCase.class.getPackage(), "web.xml", "web.xml");
        war.addAsWebResource(new StringAsset("hi"), "index.txt");
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test.jar");
        jar.addAsManifestResource(WebFragmentAbsoluteOrderingTestCase.class.getPackage(), "web-fragment.xml", "web-fragment.xml");
        war.addAsLibrary(jar);
        return war;
    }

    private String performCall(URL url, String urlPattern) throws Exception {
        return HttpRequest.get(url.toExternalForm() + urlPattern, 1000, SECONDS);
    }


    @Test
    public void testLifeCycle() throws Exception {
        Assert.assertEquals("hi", performCall(url, ""));
    }
}

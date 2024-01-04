/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jsp;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * See WFLY-2690
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JspTagTestCase {

    private static final String RESULT = "This is a header" + System.lineSeparator() +
            System.lineSeparator() +
            System.lineSeparator() +
            "<div>tag</div>" + System.lineSeparator() +
            System.lineSeparator() +
            "Static content" + System.lineSeparator();

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsWebInfResource(JspTagTestCase.class.getPackage(), "web.xml", "web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(JspTagTestCase.class.getPackage(), "tag.tag", "tags/tag.tag")
                .addAsWebResource(JspTagTestCase.class.getPackage(), "index.jsp", "index.jsp")
                .addAsWebResource(JspTagTestCase.class.getPackage(), "index.jsp", "index2.jsp")
                .addAsWebInfResource(JspTagTestCase.class.getPackage(), "header.jsp", "header.jsp");
    }

    @Test
    public void test(@ArquillianResource URL url) throws Exception {
        //we ignore line ending differences
        Assert.assertEquals(RESULT.replace("\r", ""),HttpRequest.get(url + "index.jsp", 10, TimeUnit.SECONDS).replace("\r", ""));
        Assert.assertEquals(RESULT.replace("\r", ""),HttpRequest.get(url + "index2.jsp", 10, TimeUnit.SECONDS).replace("\r", ""));
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jsp;

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

import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * See WFLY-2690
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JspTagTestCase {

    private static final String RESULT = "This is a header\n" +
            "\n" +
            "\n" +
            "<div>tag</div>\n" +
            "\n" +
            "Static content\n";

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
        Assert.assertEquals(RESULT,HttpRequest.get(url + "index.jsp", 10, TimeUnit.SECONDS));
        Assert.assertEquals(RESULT,HttpRequest.get(url + "index2.jsp", 10, TimeUnit.SECONDS));
    }
}

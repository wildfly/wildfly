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

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * See WFLY-2690
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JspTagTestCase {

    private static final StringAsset WEB_XML = new StringAsset(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_1.xsd\"\n" +
            "         version=\"3.1\">\n" +
            "</web-app>");
    
    private static final StringAsset TAG_TAG = new StringAsset(
            "<%@ tag language=\"java\" pageEncoding=\"UTF-8\" body-content=\"empty\"%>\n" +
            "<div>tag</div>");

    private static final StringAsset INDEX_JSP = new StringAsset(
            "<%@ page contentType=\"text/html;charset=UTF-8\" language=\"java\" %>\n" + 
            "<%@ taglib tagdir=\"/WEB-INF/tags\" prefix=\"my\" %>\n" +
            "<html>\n" +
            "  <head>\n" +
            "    <title></title>\n" +
            "  </head>\n" +
            "  <body>\n" +
            "    <my:tag/>\n" +
            "  </body>\n" +
            "</html>");

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsWebInfResource(WEB_XML, "web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(TAG_TAG, "tags/tag.tag")
                .addAsWebResource(INDEX_JSP, "index.jsp")
                .addAsWebResource(INDEX_JSP, "index2.jsp");
    }

    @Test
    public void test(@ArquillianResource URL url) throws Exception {
        HttpRequest.get(url + "index.jsp", 10, TimeUnit.SECONDS);
        HttpRequest.get(url + "index2.jsp", 10, TimeUnit.SECONDS);
    }
}

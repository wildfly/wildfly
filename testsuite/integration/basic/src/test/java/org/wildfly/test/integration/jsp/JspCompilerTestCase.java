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
package org.wildfly.test.integration.jsp;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * test JDK8 code constructs inside JSPs
 * See WFLY-2690
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JspCompilerTestCase {

    private static final StringAsset WEB_XML = new StringAsset(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_1.xsd\"\n" +
            "         version=\"3.1\">\n" +
            "</web-app>");

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsWebInfResource(WEB_XML, "web.xml")
                .addClasses(AnswerToEverythingComputation.class)
                .addAsWebResource(JspCompilerTestCase.class.getResource("jsp-with-lambdas.jsp"), "index.jsp");

    }

    @Test
    public void test(@ArquillianResource URL url) throws Exception {
        HttpRequest.get(url + "index.jsp", TimeoutUtil.adjust(15), TimeUnit.SECONDS);
    }
}

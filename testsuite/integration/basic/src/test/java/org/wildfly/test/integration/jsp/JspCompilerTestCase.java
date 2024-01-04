/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

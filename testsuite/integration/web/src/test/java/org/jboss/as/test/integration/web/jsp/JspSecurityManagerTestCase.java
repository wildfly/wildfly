package org.jboss.as.test.integration.web.jsp;

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

import java.net.URL;
import java.util.concurrent.TimeUnit;

@RunWith(Arquillian.class)
@RunAsClient
public class JspSecurityManagerTestCase {
    private static final StringAsset WEB_XML = new StringAsset(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
                    "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_1.xsd\"\n" +
                    "         version=\"3.1\">\n" +
                    "</web-app>");

    private static final StringAsset INDEX_JSP = new StringAsset(
            "<%@ page contentType=\"text/html;charset=UTF-8\" language=\"java\" %>\n" +
                    "<html>\n" +
                    "  <head>\n" +
                    "    <title></title>\n" +
                    "  </head>\n" +
                    "  <body>\n" +
                    "<%= System.getProperty(\"java.home\") %>" +
                    "  </body>\n" +
                    "</html>");

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "read-props.war")
                .addAsWebInfResource(WEB_XML, "web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebResource(INDEX_JSP, "index.jsp");
    }

    @Test
    public void shouldReadProperties(@ArquillianResource URL url) throws Exception {
        HttpRequest.get(url + "index.jsp", 10, TimeUnit.SECONDS);
    }
}

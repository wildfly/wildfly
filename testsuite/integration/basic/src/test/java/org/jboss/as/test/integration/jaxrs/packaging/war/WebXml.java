/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.packaging.war;

import org.jboss.shrinkwrap.api.asset.StringAsset;

/**
 * Utility class that generates a web.xml file
 *
 * TODO: replace with the SW descriptors project when it becomes available
 * @author Stuart Douglas
 */
public class WebXml {

    public static StringAsset get(String contents) {
        return new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "\n" +
                "<web-app version=\"3.0\"\n" +
                "         xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n" +
                "         metadata-complete=\"false\">\n" +
                contents +
                "</web-app>");
    }

}

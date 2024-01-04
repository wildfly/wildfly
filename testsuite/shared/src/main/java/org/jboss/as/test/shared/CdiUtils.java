/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.shared;

import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class CdiUtils {

    /**
     * Creates a {@code beans.xml} file which uses a {@code bean-discovery-mode} of "all".
     *
     * @return a {@code beans.xml} asset
     */
    public static Asset createBeansXml() {
        return new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"\n" +
                "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "       xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\"\n" +
                "       version=\"3.0\" bean-discovery-mode=\"all\">\n" +
                "</beans>");
    }
}

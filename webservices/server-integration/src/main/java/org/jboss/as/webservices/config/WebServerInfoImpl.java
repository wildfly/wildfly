/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.config;

import org.jboss.as.web.host.CommonWebServer;
import org.jboss.wsf.spi.management.WebServerInfo;

public class WebServerInfoImpl implements WebServerInfo {

    private final CommonWebServer webServer;

    public WebServerInfoImpl(CommonWebServer webServer) {
        this.webServer = webServer;
    }

    public int getPort(String protocol, boolean secure) {
        return webServer.getPort(protocol, secure);
    }
}

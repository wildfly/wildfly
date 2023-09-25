/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.config;

import org.jboss.as.web.host.CommonWebServer;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.wsf.spi.management.WebServerInfo;
import org.jboss.wsf.spi.management.WebServerInfoFactory;

public class WebServerInfoFactoryImpl extends WebServerInfoFactory {
    public WebServerInfo newWebServerInfo() {
        return new WebServerInfoImpl(ASHelper.getMSCService(CommonWebServer.SERVICE_NAME, CommonWebServer.class));
    }

}

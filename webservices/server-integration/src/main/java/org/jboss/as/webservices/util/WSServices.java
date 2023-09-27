/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.webservices.util;

import org.jboss.msc.service.ServiceName;

/**
 * WSServices
 *
 * @author alessio.soldano@jboss.com
 * @since 09-Nov-2010
 *
 */
public final class WSServices {

    public static final ServiceName WS_SERVICE = ServiceName.JBOSS.append("ws");
    public static final ServiceName CONFIG_SERVICE = WS_SERVICE.append("config");
    public static final ServiceName CLIENT_CONFIG_SERVICE = WS_SERVICE.append("client-config");
    public static final ServiceName ENDPOINT_CONFIG_SERVICE = WS_SERVICE.append("endpoint-config");
    public static final ServiceName MODEL_SERVICE = WS_SERVICE.append("model");
    public static final ServiceName ENDPOINT_SERVICE = WS_SERVICE.append("endpoint");
    public static final ServiceName ENDPOINT_DEPLOY_SERVICE = WS_SERVICE.append("endpoint-deploy");
    public static final ServiceName ENDPOINT_PUBLISH_SERVICE = WS_SERVICE.append("endpoint-publish");
    public static final ServiceName XTS_CLIENT_INTEGRATION_SERVICE = WS_SERVICE.append("xts-integration");

    //Elytron service
    public static final ServiceName ElYTRON_APP_SECURITYDOMAIN = ServiceName.of("org").append(new String[]{"wildfly", "extension","undertow", "application-security-domain"});

    private WSServices() {
        // forbidden inheritance
    }

}

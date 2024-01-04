/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.security.securitycontext;

import jakarta.servlet.ServletContext;

import io.undertow.server.HttpHandler;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;

public class MyServletExtension
        implements ServletExtension {

    private static final HttpHandler myTokenHandler = new MyDummyTokenHandler();

    @Override
    public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
        deploymentInfo.addSecurityWrapper(nextHandler -> exchange -> {
            myTokenHandler.handleRequest(exchange);
            nextHandler.handleRequest(exchange);
        });
    }

}

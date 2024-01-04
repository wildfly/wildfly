/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.support.servlet;

import java.io.IOException;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.WebConnection;

import org.jboss.as.test.integration.ee.injection.support.Alpha;
import org.jboss.as.test.integration.ee.injection.support.Charlie;
import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptorBinding;

/**
 * {@link #init(WebConnection)} method should be intercepted.
 *
 * @author Martin Kouba
 */
public class TestHttpUpgradeHandler implements HttpUpgradeHandler {

    @Inject
    private Alpha alpha;

    private Charlie charlie;

    private boolean postConstructCallbackInvoked = false;

    private boolean injectionOk = false;

    private WebConnection webConnection;

    @PostConstruct
    public void initialize() {
        postConstructCallbackInvoked = true;
    }

    @Inject
    public void setBravo(Charlie charlie) {
        this.charlie = charlie;
    }

    @ComponentInterceptorBinding
    @Override
    public void init(WebConnection wc) {
        try {
            injectionOk = (alpha != null && charlie != null && alpha.getId().equals(charlie.getAlphaId()));
        } catch (Exception e) {
            // e.printStackTrace();
        }
        try {
            this.webConnection = wc;
            this.webConnection.getInputStream().setReadListener(new TestReadListener(this));
            this.webConnection.getOutputStream().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
    }

    WebConnection getWebConnection() {
        return webConnection;
    }

    boolean isPostConstructCallbackInvoked() {
        return postConstructCallbackInvoked;
    }

    boolean isInjectionOk() {
        return injectionOk;
    }


}

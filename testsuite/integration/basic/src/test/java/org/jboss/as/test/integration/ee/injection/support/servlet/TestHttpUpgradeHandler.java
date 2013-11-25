/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ee.injection.support.servlet;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.WebConnection;

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

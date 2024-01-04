/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.optionalcomponent.cleanup;

import java.io.IOException;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
public class StandardServletAsyncWebRequest implements AsyncListener {

    /**
     * just to make non-default constructor - this is against spec and will blow up
     */
    public StandardServletAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
    }

    @Override
    public void onError(AsyncEvent event) throws IOException {
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
    }

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
    }

}

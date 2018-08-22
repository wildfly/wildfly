package org.jboss.as.test.integration.weld.webcomponents;

import javax.servlet.AsyncListener;
import javax.servlet.AsyncEvent;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

public class StandardServletAsyncWebRequest implements AsyncListener {
    /**
     * just to make non-default constructor as in
     * org.springframework.web.context.request.async.StandardServletAsyncWebRequest
     */
    public StandardServletAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
    }

    // ---------------------------------------------------------------------
    // Implementation of AsyncListener methods
    // ---------------------------------------------------------------------

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

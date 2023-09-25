/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.suspend;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A servlet that is used to test different way of setting and retrieving cookies.
 *
 * @author prabhat.jha@jboss.com
 */
@WebServlet(name = "ShutdownServlet", urlPatterns = { "/ShutdownServlet" })
public class ShutdownServlet extends HttpServlet {

    private static final long serialVersionUID = -5891682551205336273L;

    public static final CountDownLatch requestLatch  = new CountDownLatch(1);
    public static final String TEXT = "Running Request";

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            requestLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        response.getWriter().write(TEXT);
        response.getWriter().close();
    }

}

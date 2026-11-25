/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.timeout;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Collectors;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = ListenerTimeoutTestCaseBase.WRITE_SERVLET_PATH)
public class WriteServlet extends HttpServlet {
    @EJB(mappedName = "java:global/sockettimeout/TimeoutLogBean!org.jboss.as.test.integration.web.timeout.TimeoutLog")
    private TimeoutLog bean;
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //TOs are opportunistic.
        //FLOW:
        //1. incomming message - timestamp taken
        //2. 100-Confinue sent back to keep session alive
        //3. wait over timestamp+RW TO
        //4. subsequent request on the same session
        //5. Receive header, terminate writes/socket, deliver to servlet - yep, its a hack.
        bean.receivedMessage();
        try {
            req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            resp.setStatus(100);
            resp.flushBuffer();
            bean.sentResponse();
        } catch (UncheckedIOException cce) {
            bean.failedIO();
        }
    }

}

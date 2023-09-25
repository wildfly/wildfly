/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.deploymentoverlay;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2015 Red Hat, inc.
 */
@WebServlet(name = "OverlayServlet", urlPatterns = {"/overlay/"}, loadOnStartup = 1)
public class OverlayServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (InputStream from = getClass().getClassLoader().getResourceAsStream("wildcard-new-file")) {
            OutputStream output = resp.getOutputStream();
            final byte[] buffer = new byte[4096];
            int read = 0;
            while ((read = from.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
        }

    }

}

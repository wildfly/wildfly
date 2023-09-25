/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.threads;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

/**
 */
@WebServlet(name = "RaceyServlet", urlPatterns = {"/race"})
public class RaceyServlet extends HttpServlet {

    private int value;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int value = this.value;
        //incremement the value, with a little sleep to increase the chance of a racey update
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {

        }
        this.value = value + 1;
        Writer writer = resp.getWriter();
        writer.write("" + value);
    }
}

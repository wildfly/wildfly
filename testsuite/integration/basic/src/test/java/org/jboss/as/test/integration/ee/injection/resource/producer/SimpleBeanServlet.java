/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.producer;

import java.io.IOException;
import java.io.Writer;

import jakarta.annotation.Resource;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@SuppressWarnings("serial")
@WebServlet(name = "SimpleBeanServlet", urlPatterns = { "/simple" })
public class SimpleBeanServlet extends HttpServlet {

    @Resource
    SimpleManagedBean bean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Writer writer = resp.getWriter();
        writer.write(bean.getDriverName());
        writer.close();
    }
}

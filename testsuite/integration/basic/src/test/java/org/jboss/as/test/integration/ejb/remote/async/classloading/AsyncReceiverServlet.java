/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.async.classloading;

import java.io.IOException;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author baranowb
 *
 */
@WebServlet(name = "AsyncReceiverServer", urlPatterns = { "/*" })
public class AsyncReceiverServlet extends HttpServlet {

    @EJB(lookup="java:global/wildName/ejbjar/AsyncRemoteEJB!org.jboss.as.test.integration.ejb.remote.async.classloading.AsyncRemote")
    private AsyncRemote remoter;
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if(req.getParameter("null")!=null){
                ReturnObject value = remoter.testAsyncNull("NULL!").get();
            } else {
                ReturnObject value = remoter.testAsync("Trololo").get();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
        resp.setStatus(200);
        resp.flushBuffer();
    }

}

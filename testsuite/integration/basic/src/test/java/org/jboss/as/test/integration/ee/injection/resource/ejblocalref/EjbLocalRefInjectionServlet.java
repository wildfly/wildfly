/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.ejblocalref;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Stuart Douglas
 */
@WebServlet(name="ejbLocalRef",urlPatterns = {"/ejbLocalRef"})
public class EjbLocalRefInjectionServlet extends HttpServlet {

    private Hello named;

    private Hello simpleHelloBean;

    public void setSimple(Hello hello) {
        simpleHelloBean = hello;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(req.getParameter("type").equals("named")) {
            resp.getWriter().append(named.sayHello()).flush();
        } else {
            resp.getWriter().append(simpleHelloBean.sayHello());
        }
        resp.getWriter().close();
    }

}

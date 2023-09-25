/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.client.descriptor.passbyvalue;

import java.io.IOException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 * @author baranowb
 *
 */
@WebServlet(urlPatterns = "/test")
public class ClientPassByValueTestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final DummySerializableObject DUMMY_SERIALIZABLE_OBJECT = new DummySerializableObject();
    public static final String FLIP = "flip";

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        final String flip = request.getParameter(FLIP);
        if (flip != null && Boolean.parseBoolean(flip)) {
            InitialContext ctx = null;
            try {
                ctx = new InitialContext();

                TestEJBRemote ejb = (TestEJBRemote) ctx.lookup("ejb:/TestWAR//TestEJB!org.jboss.as.test.integration.ejb.client.descriptor.passbyvalue.TestEJBRemote");

                final String objectRefInside = ejb.getObjectReference(DUMMY_SERIALIZABLE_OBJECT);
                write(response, objectRefInside);
            } catch (NamingException ne) {
                throw new RuntimeException(ne);
            } finally {
                if (ctx != null) {
                    try {
                        ctx.close();
                    } catch (NamingException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            write(response, DUMMY_SERIALIZABLE_OBJECT.toString());
        }
    }

    private static void write(HttpServletResponse writer, String message) {
        try {
            writer.getWriter().write(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

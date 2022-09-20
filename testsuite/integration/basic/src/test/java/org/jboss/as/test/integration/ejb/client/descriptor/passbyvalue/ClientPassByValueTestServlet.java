/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

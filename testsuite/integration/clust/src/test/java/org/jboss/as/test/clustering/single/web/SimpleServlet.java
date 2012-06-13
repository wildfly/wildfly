/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.single.web;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { "/simple" })
public class SimpleServlet extends HttpServlet {
    private static final long serialVersionUID = -592774116315946908L;
    public static final String REQUEST_DURATION_PARAM = "requestduration";
    public static final String HEADER_SERIALIZED = "serialized";
    public static final String URL = "simple";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(true);
        Custom custom = (Custom) session.getAttribute("test");
        if (custom == null) {
            custom = new Custom(1);
            session.setAttribute("test", custom);
        } else {
            custom.increment();
        }
        resp.setIntHeader("value", custom.getValue());
        resp.setHeader(HEADER_SERIALIZED, Boolean.toString(custom.wasSerialized()));

        // Long running request?
        if (req.getParameter(REQUEST_DURATION_PARAM) != null) {
            int duration = Integer.valueOf(req.getParameter(REQUEST_DURATION_PARAM));
            try {
                Thread.sleep(duration);
            } catch (InterruptedException ex) {
            }
        }

        resp.getWriter().write("Success");
    }

    public static class Custom implements Serializable {
        private static final long serialVersionUID = -5129400250276547619L;
        private transient boolean serialized = false;
        private int value;

        public Custom(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public void increment() {
            this.value += 1;
        }

        public boolean wasSerialized() {
            return this.serialized;
        }

        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            this.serialized = true;
        }

        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            this.serialized = true;
        }
    }
}

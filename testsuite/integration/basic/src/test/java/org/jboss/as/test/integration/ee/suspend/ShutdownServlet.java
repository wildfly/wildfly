/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ee.suspend;


import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "ShutdownServlet", urlPatterns = { "/ShutdownServlet" })
public class ShutdownServlet extends HttpServlet {

    private static final long serialVersionUID = -5891682551205336273L;

    public static final CountDownLatch requestLatch  = new CountDownLatch(1);
    public static final String TEXT = "Running Request";

    @Resource
    private ManagedExecutorService executorService;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    requestLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        response.getWriter().write(TEXT);
        response.getWriter().close();
    }

}

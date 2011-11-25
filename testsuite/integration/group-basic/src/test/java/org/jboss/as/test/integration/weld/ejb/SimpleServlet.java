/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.weld.ejb;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.beans.XMLEncoder;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.stdio.WriterOutputStream;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@WebServlet(name="SimpleServlet", urlPatterns={"/simple"})
public class SimpleServlet extends HttpServlet {
    static ThreadLocal<String> propagated = new ThreadLocal<String>();

    @Inject
    private SimpleStatefulSessionBean bean;

    private String sharedContext;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String msg = req.getParameter("input");

        // the first call needs to be concurrent
        //bean.setMessage(msg);

        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final CountDownLatch latch = new CountDownLatch(1);
        final Future<String>[] futures = new Future[2];
        for (int i = 0; i < futures.length; i++) {
            futures[i] = executor.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    try {
                        return bean.echo(latch, msg);
                    }
                    finally {
                        // the second concurrent call will throw ConcurrentAccessException
                        // so now we make the first call proceed
                        latch.countDown();
                    }
                }
            });
        }

        final List<String> results = new LinkedList<String>();
        final List<Throwable> exceptions = new LinkedList<Throwable>();
        for (int i = 0; i < futures.length; i++) {
            try {
                String result = futures[i].get(10, SECONDS);
                results.add(result);
            } catch(ExecutionException e) {
                e.printStackTrace();
                exceptions.add(e.getCause());
            } catch (InterruptedException e) {
                e.printStackTrace();
                exceptions.add(e);
            } catch (TimeoutException e) {
                e.printStackTrace();
                exceptions.add(e);
            }
        }

        // make a 'nice' report
        PrintWriter writer = resp.getWriter();
        XMLEncoder encoder = new XMLEncoder(new WriterOutputStream(writer));
        encoder.writeObject(results);
        encoder.writeObject(exceptions);
        encoder.writeObject(sharedContext);
        encoder.close();
    }

    @PostConstruct
    public void postConstruct() {
        this.sharedContext = propagated.get();
    }
}

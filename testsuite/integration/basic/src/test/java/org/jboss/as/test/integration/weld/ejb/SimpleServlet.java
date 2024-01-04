/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

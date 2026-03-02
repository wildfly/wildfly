/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.servlet.async;

import java.io.IOException;
import java.io.Serializable;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.ejb.EJB;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = ("/init/*"), name = "PathMappingServlet", asyncSupported = true)
public class AsyncNonDispatchServlet extends HttpServlet {

    @EJB(mappedName = "java:global/asynctimeout/AsyncLeashBean!org.jboss.as.test.integration.web.servlet.async.AsyncLeash")
    private AsyncLeash leash;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final AsyncContext ctx = req.startAsync();
        ctx.addListener(new AsyncGuard(), req, resp);
        String t = req.getParameter("timeout");
        if (t != null) {
            ctx.setTimeout(Long.parseLong(t));
        }
        leash.init(ctx.getTimeout());
        Thread tr = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(ctx.getTimeout() + 1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        tr.start();

    }

    private class AsyncGuard implements AsyncListener, Serializable {
        public void onComplete(AsyncEvent event) throws IOException {
            try {
                lookupEJB().onComplete();
            } catch (NamingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void onTimeout(AsyncEvent event) throws IOException {
            try {
                lookupEJB().onTimeout(event.getThrowable());
            } catch (NamingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void onError(AsyncEvent event) throws IOException {
            try {
                lookupEJB().onError(event.getThrowable());
            } catch (NamingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void onStartAsync(AsyncEvent event) throws IOException {
            try {
                lookupEJB().onStartAsync();
            } catch (NamingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    public static AsyncLeash lookupEJB() throws NamingException {
        final Context context = getInitialContext();
        return (AsyncLeash) context
                .lookup("ejb:/asynctimeout/AsyncLeashBean!org.jboss.as.test.integration.web.servlet.async.AsyncLeash");
    }

    private static InitialContext getInitialContext() throws NamingException {
        final Hashtable<String, String> jndiProperties = new Hashtable<String, String>();
        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.as.naming.InitialContextFactory");
        jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        return new InitialContext(jndiProperties);
    }
}

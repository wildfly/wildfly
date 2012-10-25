/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Transition;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Utility servlet that waits until a specified service reaches a specific state.
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { ServiceListenerServlet.SERVLET_PATH })
public class ServiceListenerServlet extends HttpServlet {
    private static final long serialVersionUID = -879311373457718598L;
    public static final String SERVLET_NAME = "service-listener";
    public static final String SERVLET_PATH = "/" + SERVLET_NAME;
    public static final String SERVICE = "service";
    public static final String STATE = "state";
    public static final String TIMEOUT = "timeout";
    public static final long DEFAULT_TIMEOUT = 5000;

    public static URI createURI(URL baseURL, ServiceName name, ServiceController.State state) throws UnsupportedEncodingException, URISyntaxException {
        return baseURL.toURI().resolve(new StringBuilder(SERVLET_NAME).append('?').append(SERVICE).append('=').append(URLEncoder.encode(name.getCanonicalName(), "UTF-8")).append('&').append(STATE).append('=').append(state.name()).toString());
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServiceName name = ServiceName.parse(this.getRequiredParameter(req, SERVICE));
        String timeoutParameter = req.getParameter(TIMEOUT);
        long timeout = (timeoutParameter != null) ? Integer.parseInt(timeoutParameter) : DEFAULT_TIMEOUT;
        ServiceController.State expectedState = ServiceController.State.valueOf(this.getRequiredParameter(req, STATE));
        ServiceRegistry registry = ServiceContainerHelper.getCurrentServiceContainer();
        long start = System.currentTimeMillis();
        long now = start;
        long stop = start + timeout;
        ServiceController<?> controller = registry.getService(name);
        // Potentially wait until specified service is registered
        while ((controller == null) && (now < stop)) {
            Thread.yield();
            now = System.currentTimeMillis();
            controller = registry.getService(name);
        }
        if (controller == null) {
            throw new ServletException(String.format("Failed to locate %s within %d ms", name, timeout));
        }
        ServiceListener<Object> listener = new AbstractServiceListener<Object>() {
            @Override
            public void transition(ServiceController<? extends Object> controller, Transition transition) {
                synchronized (ServiceListenerServlet.this) {
                    ServiceListenerServlet.this.notify();
                }
            }
            
        };
        try
        {
            controller.addListener(listener);
            synchronized (this) {
                ServiceController.State state = controller.getState();
                while (state != expectedState) {
                    this.wait(stop - now);
                    now = System.currentTimeMillis();
                    if (now >= stop) {
                        throw new InterruptedException(String.format("%s failed to reach %s state in %s ms.  Current state is %s", name, expectedState, timeout, state));
                    }
                    state = controller.getState();
                }
                System.out.println(String.format("%s successfully reached %s state within %d ms.", name, expectedState, now - start));
            }
        } catch (InterruptedException e) {
            throw new ServletException(e);
        } finally {
            controller.removeListener(listener);
        }
    }
    
    private String getRequiredParameter(HttpServletRequest request, String name) throws ServletException {
        String value = request.getParameter(name);
        if (value == null) {
            throw new ServletException(String.format("No '%s' parameter specified", name));
        }
        return value;
    }
}

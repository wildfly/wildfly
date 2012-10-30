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
package org.jboss.as.test.clustering;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartException;

/**
 * Utility servlet that waits until the specified cluster establishes a specific cluster membership.
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { ViewChangeListenerServlet.SERVLET_PATH })
@Listener(sync = false)
public class ViewChangeListenerServlet extends HttpServlet {
    public static final String SERVLET_NAME = "membership";
    public static final String SERVLET_PATH = "/" + SERVLET_NAME;
    private static final long serialVersionUID = -4382952409558738642L;
    public static final long TIMEOUT = 10000;
    public static final String CLUSTER = "cluster";
    public static final String MEMBERS = "members";
    
    public static URI createURI(URL baseURL, String cluster, String... members) throws URISyntaxException {
        StringBuilder builder = new StringBuilder(baseURL.toURI().resolve(SERVLET_NAME).toString());
        builder.append("?").append(CLUSTER).append("=").append(cluster);
        for (String member: members) {
            builder.append("&").append(MEMBERS).append("=").append(member);
        }
        return URI.create(builder.toString());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String cluster = req.getParameter(CLUSTER);
        if (cluster == null) {
            throw new ServletException(String.format("No '%s' parameter specified", CLUSTER));
        }
        String[] names = req.getParameterValues(MEMBERS);
        Set<String> expectedMembers = new TreeSet<String>();
        if (names != null) {
            for (String name: names) {
                expectedMembers.add(name + "/" + cluster);
            }
        }
        ServiceRegistry registry = ServiceContainerHelper.getCurrentServiceContainer();
        ServiceController<?> controller = registry.getService(ServiceName.JBOSS.append("infinispan", cluster));
        if (controller == null) {
            throw new ServletException(String.format("Failed to locate service for cluster '%s'", cluster));
        }
        try {
            EmbeddedCacheManager manager = ServiceContainerHelper.getValue(controller, EmbeddedCacheManager.class);
            manager.addListener(this);
            try
            {
                long start = System.currentTimeMillis();
                long now = start;
                long timeout = start + TIMEOUT;
                synchronized (this) {
                    Set<String> members = this.getMembers(manager);
                    while (!expectedMembers.equals(members)) {
                        System.out.println(expectedMembers + " != " + members + ", waiting for a view change event...");
                        this.wait(timeout - now);
                        now = System.currentTimeMillis();
                        if (now >= timeout) {
                            throw new InterruptedException(String.format("Cluster '%s' failed to establish view %s within %d ms.  Current view is: %s", cluster, expectedMembers, TIMEOUT, members));
                        }
                        members = this.getMembers(manager);
                    }
                    System.out.println(String.format("Cluster '%s' successfully established view %s within %d ms.", cluster, expectedMembers, now - start));
                }
            } catch (InterruptedException e) {
                throw new ServletException(e);
            } finally {
                manager.removeListener(this);
            }
        } catch (StartException e) {
            throw new ServletException(e);
        }
    }

    private Set<String> getMembers(EmbeddedCacheManager manager) {
        Set<String> members = new TreeSet<String>();
        for (Address address: manager.getMembers()) {
            members.add(address.toString());
        }
        return members;
    }

    @ViewChanged
    public void viewChanged(ViewChangedEvent event) {
        System.out.println("View changed event received: " + event);
        synchronized (this) {
            this.notify();
        }
    }
}

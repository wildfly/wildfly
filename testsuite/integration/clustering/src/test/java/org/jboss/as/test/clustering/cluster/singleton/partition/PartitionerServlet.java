/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.clustering.cluster.singleton.partition;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jgroups.Channel;
import org.jgroups.View;
import org.jgroups.protocols.DISCARD;
import org.jgroups.protocols.TP;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.stack.ProtocolStack;

/**
 * Servlet used to simulate network partitions. Responds to {@code /partitioner?partition=true} by inserting DISCARD protocol to the
 * stack and {@code /partitioner?partition=false} by removing previously inserted DISCARD protocol.
 *
 * @author Radoslav Husar
 */
@WebServlet(urlPatterns = {PartitionerServlet.SERVLET_PATH})
public class PartitionerServlet extends HttpServlet {

    public static final String SERVLET_PATH = "partitioner";
    private static final String PARTITION = "partition";

    public static URI createURI(URL baseURL, boolean partition) throws URISyntaxException {
        return baseURL.toURI().resolve(SERVLET_PATH + '?' + PARTITION + '=' + partition);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean partition = Boolean.valueOf(request.getParameter(PARTITION));

        this.log("Simulating network partitions? " + partition);

        @SuppressWarnings("unchecked")
        ServiceController<Channel> service = (ServiceController<Channel>) CurrentServiceContainer.getServiceContainer().getService(ServiceName.parse("org.wildfly.clustering.jgroups.default-channel"));
        try {
            Channel channel = service.awaitValue();

            if (partition) {
                // Simulate partitions by injecting DISCARD protocol
                DISCARD discard = new DISCARD();
                discard.setDiscardAll(true);
                channel.getProtocolStack().insertProtocol(discard, ProtocolStack.ABOVE, TP.class);

                // Speed up partitioning
                GMS gms = (GMS) channel.getProtocolStack().findProtocol(GMS.class);
                View view = View.create(channel.getAddress(), gms.getViewId().getId() + 1, channel.getAddress());
                gms.installView(view);
            } else {
                channel.getProtocolStack().removeProtocol(DISCARD.class);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }

        response.getWriter().write("Success");
    }
}

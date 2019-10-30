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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.View;
import org.jgroups.protocols.DISCARD;
import org.jgroups.protocols.TP;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.stack.ProtocolStack;
import org.wildfly.clustering.service.PassiveServiceSupplier;

/**
 * Servlet used to simulate network partitions. Responds to {@code /partition?partition=true} by inserting DISCARD protocol to the stack
 * and installing new view directly on the {@link GMS} and responds to {@code /partition?partition=false} by removing previously inserted
 * {@link DISCARD} protocol and passing MERGE event up the stack.
 * <p>
 * Note that while it would be desirable for the tests to leave splitting and merging to the servers themselves, this is not practical in a
 * test suite. While FD/VERIFY_SUSPECT/GMS can be be configured to detect partitions quickly the MERGE3 handles merges within randomized
 * intervals and uses unreliable channel which can easily take several minutes for merge to actually happen. Also, speeds up the test
 * significantly.
 *
 * @author Radoslav Husar
 */
@WebServlet(urlPatterns = {PartitionServlet.PARTITION})
public class PartitionServlet extends HttpServlet {
    private static final long serialVersionUID = 3034138469210308974L;

    private static final long VIEWS_TIMEOUT = 3_000;
    public static final String PARTITION = "partition";

    private static Map<Address, View> mergeViews;

    public static URI createURI(URL baseURL, boolean partition) throws URISyntaxException {
        return baseURL.toURI().resolve(PARTITION + '?' + PARTITION + '=' + partition);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean partition = Boolean.valueOf(request.getParameter(PARTITION));

        this.log("Simulating network partitions? " + partition);

        JChannel channel = new PassiveServiceSupplier<JChannel>(CurrentServiceContainer.getServiceContainer(), ServiceName.parse("org.wildfly.clustering.jgroups.default-channel")).get();
        try {
            if (partition) {
                // Store views for future merge event
                GMS gms = channel.getProtocolStack().findProtocol(GMS.class);
                mergeViews = new HashMap<>();
                channel.getView().getMembers().forEach(
                        address -> mergeViews.put(address, View.create(address, gms.getViewId().getId() + 1, address))
                );
                // Wait a few seconds to ensure everyone stored a full view
                Thread.sleep(VIEWS_TIMEOUT);

                // Simulate partitions by injecting DISCARD protocol
                DISCARD discard = new DISCARD();
                discard.setDiscardAll(true);
                channel.getProtocolStack().insertProtocol(discard, ProtocolStack.Position.ABOVE, TP.class);

                // Speed up partitioning
                View view = View.create(channel.getAddress(), gms.getViewId().getId() + 1, channel.getAddress());
                gms.installView(view);
            } else {
                channel.getProtocolStack().removeProtocol(DISCARD.class);
                // Wait a few seconds for the other node to remove DISCARD so it does not discard our MERGE request
                Thread.sleep(VIEWS_TIMEOUT);

                // Since the coordinator is determined by ordering the address in org.jgroups.protocols.pbcast.Merger#determineMergeLeader
                // let just all nodes send the merge..
                this.log("Passing event up the stack: " + new Event(Event.MERGE, mergeViews));

                GMS gms = channel.getProtocolStack().findProtocol(GMS.class);
                gms.up(new Event(Event.MERGE, mergeViews));
                mergeViews = null;
            }

            response.getWriter().write("Success");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}

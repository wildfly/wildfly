/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.singleton.partition;

import java.io.IOException;
import java.io.Serial;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.View;
import org.jgroups.protocols.BasicTCP;
import org.jgroups.protocols.DISCARD;
import org.jgroups.protocols.FD_SOCK2;
import org.jgroups.protocols.TP;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.stack.ProtocolStack;

/**
 * Servlet used to simulate network partitions. Responds to {@code /partition?partition=true} by inserting DISCARD protocol to the stack
 * and installing new view directly on the {@link GMS} and responds to {@code /partition?partition=false} by removing previously inserted
 * {@link DISCARD} protocol and passing MERGE event up the stack.
 * <p>
 * Note that while it would be desirable for the tests to leave splitting and merging to the servers themselves, this is not practical in a
 * test suite. While FD/VERIFY_SUSPECT/GMS can be configured to detect partitions quickly the MERGE3 handles merges within randomized
 * intervals and uses unreliable channel which can easily take several minutes for merge to actually happen. Also, speeds up the test
 * significantly.
 *
 * @author Radoslav Husar
 */
@WebServlet(urlPatterns = {PartitionServlet.PARTITION})
public class PartitionServlet extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 3034138469210308974L;

    private static final long VIEWS_TIMEOUT = 3_000;
    public static final String PARTITION = "partition";

    private static Map<Address, View> mergeViews;

    public static URI createURI(URL baseURL, boolean partition) throws URISyntaxException {
        return baseURL.toURI().resolve(PARTITION + '?' + PARTITION + '=' + partition);
    }

    @Resource(lookup = "java:jboss/jgroups/channel/default")
    private JChannel channel;

    @Override
    public void init() throws ServletException {
        ProtocolStack stack = this.channel.getProtocolStack();
        TP transport = stack.getTransport();
        if (transport instanceof BasicTCP) {
            BasicTCP tcp = (BasicTCP) transport;
            if (!(tcp.enableSuspectEvents() ^ (stack.findProtocol(FD_SOCK2.class) != null))) {
                // Verify that the stack uses transport-based failure detection or contains a socket-based failure detection protocol, but not both.
                throw new ServletException(String.format("Protocol stack for %s should contain exactly one socket-based failure detection mechanism", this.channel.getClusterName()));
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean partition = Boolean.parseBoolean(request.getParameter(PARTITION));

        this.log("Simulating network partitions? " + partition);

        try {
            if (partition) {
                // Store views for future merge event
                GMS gms = this.channel.getProtocolStack().findProtocol(GMS.class);
                mergeViews = new HashMap<>();
                this.channel.getView().getMembers().forEach(
                        address -> mergeViews.put(address, View.create(address, gms.getViewId().getId() + 1, address))
                );
                // Wait a few seconds to ensure everyone stored a full view
                Thread.sleep(VIEWS_TIMEOUT);

                // Simulate partitions by injecting DISCARD protocol
                DISCARD discard = new DISCARD();
                discard.discardAll(true);
                this.channel.getProtocolStack().insertProtocol(discard, ProtocolStack.Position.ABOVE, TP.class);

                // Speed up partitioning
                View view = View.create(this.channel.getAddress(), gms.getViewId().getId() + 1, this.channel.getAddress());
                gms.installView(view);
            } else {
                this.channel.getProtocolStack().removeProtocol(DISCARD.class);
                // Wait a few seconds for the other node to remove DISCARD so it does not discard our MERGE request
                Thread.sleep(VIEWS_TIMEOUT);

                // Since the coordinator is determined by ordering the address in org.jgroups.protocols.pbcast.Merger#determineMergeLeader
                // let just all nodes send the merge..
                this.log("Passing event up the stack: " + new Event(Event.MERGE, mergeViews));

                GMS gms = this.channel.getProtocolStack().findProtocol(GMS.class);
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

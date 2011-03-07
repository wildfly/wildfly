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
package org.jboss.as.domain.http.server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

import org.jboss.as.controller.ModelController;

import com.sun.net.httpserver.HttpServer;

/**
 * The general HTTP server for handling management API requests.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ManagementHttpServer {

    private HttpServer httpServer;

    private List<ManagementHttpHandler> handlers = new LinkedList<ManagementHttpHandler>();

    private ManagementHttpServer(HttpServer httpServer) {
        this.httpServer = httpServer;
    }

    private void addHandler(ManagementHttpHandler handler) {
        handlers.add(handler);
    }

    public void start() {
        for (ManagementHttpHandler current : handlers) {
            current.start(httpServer);
        }
        httpServer.start();
    }

    public void stop() {
        httpServer.stop(0);
        for (ManagementHttpHandler current : handlers) {
            current.stop(httpServer);
        }
    }

    public static ManagementHttpServer create(InetSocketAddress socket, int backlog, ModelController modelController, Executor executor)
            throws IOException {
        HttpServer server = HttpServer.create(socket, backlog);
        server.setExecutor(executor);
        ManagementHttpServer managementHttpServer = new ManagementHttpServer(server);
        managementHttpServer.addHandler(new DomainApiHandler(modelController));
        managementHttpServer.addHandler(new ConsoleHandler());

        return managementHttpServer;
    }

}

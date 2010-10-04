/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server;

import org.jboss.as.model.ServerModel;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Exposes the {@link ServerModel} via a {@link Service}.
 *
 * @author Brian Stansberry
 */
public class ServerModelService implements Service<ServerModel> {

    private final ServerModel serverModel;

    /**
     * Creates an instance of ServerModelService and configures the BatchBuilder to install it.
     *
     * @param serverModel the ServerModel to expose
     * @param batchBuilder service batch builder to use to install the service.  Cannot be {@code null}
     */
    public static void addService(final ServerModel serverModel, final BatchBuilder batchBuilder) {
        ServerModelService service = new ServerModelService(serverModel);
        batchBuilder.addService(ServerModel.SERVICE_NAME, service);
    }

    private ServerModelService(ServerModel serverModel) {
        if (serverModel == null)
            throw new IllegalArgumentException("serverModel is null");
        this.serverModel = serverModel;
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public ServerModel getValue() throws IllegalStateException {
        return serverModel;
    }

}

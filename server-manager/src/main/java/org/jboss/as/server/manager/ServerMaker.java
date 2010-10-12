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

package org.jboss.as.server.manager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.JvmElement;
import org.jboss.as.model.PropertiesElement;
import org.jboss.as.model.ServerFactory;
import org.jboss.as.process.ProcessManagerProtocol.OutgoingPmCommandHandler;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassTable;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerMaker {

    private final ProcessManagerSlave processManagerSlave;

    private final ServerManagerEnvironment environment;

    private final CommunicationVariables communicationVariables;

    public ServerMaker(ServerManagerEnvironment environment,
            ProcessManagerSlave processManagerSlave,
            OutgoingPmCommandHandler messageHandler,
            CommunicationVariables communicationVariables) {

        if (environment == null) {
            throw new IllegalArgumentException("environment is null");
        }
        this.environment = environment;

        if (processManagerSlave == null) {
            throw new IllegalArgumentException("processManagerSlave is null");
        }
        this.processManagerSlave = processManagerSlave;

        if (messageHandler == null) {
            throw new IllegalArgumentException("messageHandler is null");
        }
        if (communicationVariables == null) {
            throw new IllegalArgumentException("communicationVariables is null");
        }
        this.communicationVariables = communicationVariables;
    }

    void addProcess(Server server, JvmElement jvmElement) throws IOException {
        final String serverProcessName = server.getServerProcessName();
        String serverName = server.getServerName();
        List<String> command = server.getServerLaunchCommand();

        Map<String, String> env = getServerLaunchEnvironment(jvmElement);

        //Add to process manager
        processManagerSlave.addProcess(serverProcessName, command, env, environment.getHomeDir().getAbsolutePath());
        final List<AbstractServerModelUpdate<?>> updateList = new ArrayList<AbstractServerModelUpdate<?>>();
        ServerFactory.combine(null, null, serverName, updateList);
        // TODO - not efficient but it will work for now
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(32768);
        final MarshallerFactory marshallerFactory = Marshalling.getMarshallerFactory("river");
        final MarshallingConfiguration configuration = new MarshallingConfiguration();
        configuration.setVersion(2);
        configuration.setClassTable(ModularClassTable.getInstance());
        final Marshaller marshaller = marshallerFactory.createMarshaller(configuration);
        marshaller.start(Marshalling.createByteOutput(baos));
        processManagerSlave.sendStdin(serverProcessName, baos.toByteArray());

    }

    private Map<String, String> getServerLaunchEnvironment(JvmElement jvm) {
        Map<String, String> env = null;
        PropertiesElement pe = jvm.getEnvironmentVariables();
        if (pe != null) {
            env = pe.getProperties();
        }
        else {
            env = Collections.emptyMap();
        }
        return env;
    }
}

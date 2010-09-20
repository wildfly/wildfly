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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;

import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ServerModel;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;
import org.jboss.staxmapper.XMLMapper;

/**
 * The standalone server.
 *
 * @author Emanuel Muckenhuber
 */
public class StandaloneServer extends AbstractServer {

    private static final String STANDALONE_XML = "standalone.xml";
    private final StandardElementReaderRegistrar extensionRegistrar;

    protected StandaloneServer(ServerEnvironment environment) {
        super(environment);
        extensionRegistrar = StandardElementReaderRegistrar.Factory.getRegistrar();
    }

    public void start() throws ServerStartException {
        final File standalone = new File(getEnvironment().getServerConfigurationDir(), STANDALONE_XML);
        if(! standalone.isFile()) {
            throw new ServerStartException("File " + standalone.getAbsolutePath()  + " does not exist.");
        }
        if(! standalone.canWrite() ) {
            throw new ServerStartException("File " + standalone.getAbsolutePath()  + " is not writeable.");
        }
        final ParseResult<ServerModel> parseResult = new ParseResult<ServerModel>();
        try {
            final XMLMapper mapper = XMLMapper.Factory.create();
            extensionRegistrar.registerStandardStandaloneReaders(mapper);
            mapper.parseDocument(parseResult, XMLInputFactory.newInstance().createXMLStreamReader(new BufferedReader(new FileReader(standalone))));
        } catch (Exception e) {
            throw new ServerStartException("Caught exception during processing of standalone.xml", e);
        }

        start(parseResult.getResult());
        // TODO remove life thread
        new Thread() { {
                setName("Server Life Thread");
                setDaemon(false);
                setPriority(MIN_PRIORITY);
            }

            public void run() {
                for (;;)
                    try {
                        sleep(1000000L);
                    } catch (InterruptedException ignore) {
                        //
                    }
            }
        }.start();
    }

    ServerStartupListener.Callback createListenerCallback() {
        return new ServerStartupListener.Callback() {
            public void run(Map<ServiceName, StartException> serviceFailures, long elapsedTime, int totalServices, int onDemandServices, int startedServices) {
                if(serviceFailures.isEmpty()) {
                    log.infof("JBoss AS started in %dms. - Services [Total: %d, On-demand: %d. Started: %d]", elapsedTime, totalServices, onDemandServices, startedServices);
                } else {
                    final StringBuilder buff = new StringBuilder(String.format("JBoss AS server start failed. Attempted to start %d services in %dms", totalServices, elapsedTime));
                    buff.append("\nThe following services failed to start:\n");
                    for(Map.Entry<ServiceName, StartException> entry : serviceFailures.entrySet()) {
                        buff.append(String.format("\t%s => %s\n", entry.getKey(), entry.getValue().getMessage()));
                    }
                    log.error(buff.toString());
                }
            }
        };
    }

}


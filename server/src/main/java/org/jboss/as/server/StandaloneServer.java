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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLInputFactory;

import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.staxmapper.XMLMapper;

/**
 * The standalone server.
 *
 * @author Emanuel Muckenhuber
 */
public class StandaloneServer {

    private static final String STANDALONE_XML = "standalone.xml";
    private final StandardElementReaderRegistrar extensionRegistrar;

    static final Logger log = Logger.getLogger("org.jboss.as.server");

    private final ServerEnvironment environment;

    public StandaloneServer(ServerEnvironment environment) {
        if (environment == null) {
            throw new IllegalArgumentException("bootstrapConfig is null");
        }
        this.environment = environment;
        extensionRegistrar = StandardElementReaderRegistrar.Factory.getRegistrar();
    }

    public void start() throws ServerStartException {
       start(Collections.<ServiceActivator>emptyList());
    }

    public void start(List<ServiceActivator> serviceActivators) throws ServerStartException {
        final File standalone = new File(environment.getServerConfigurationDir(), STANDALONE_XML);
        if(! standalone.isFile()) {
            throw new ServerStartException("File " + standalone.getAbsolutePath()  + " does not exist.");
        }
        if(! standalone.canWrite() ) {
            throw new ServerStartException("File " + standalone.getAbsolutePath()  + " is not writable.");
        }
        final List<AbstractServerModelUpdate<?>> updates = new ArrayList<AbstractServerModelUpdate<?>>();
        try {
            final XMLMapper mapper = XMLMapper.Factory.create();
            extensionRegistrar.registerStandardStandaloneReaders(mapper);
            mapper.parseDocument(updates, XMLInputFactory.newInstance().createXMLStreamReader(new BufferedInputStream(new FileInputStream(standalone))));
        } catch (Exception e) {
            throw new ServerStartException("Caught exception during processing of standalone.xml", e);
        }

        final ServerStartTask startTask = new ServerStartTask(0, serviceActivators, updates, environment);
        startTask.run(Collections.<ServiceActivator>emptyList());

        // TODO remove life thread
        new Thread() { {
                setName("Server Life Thread");
                setDaemon(false);
                setPriority(MIN_PRIORITY);
            }

            @Override
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
}


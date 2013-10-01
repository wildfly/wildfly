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

package org.jboss.as.naming.service;

import static org.jboss.as.naming.NamingLogger.ROOT_LOGGER;
import static org.jboss.as.naming.NamingMessages.MESSAGES;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NameParser;
import javax.naming.NamingException;

import org.jboss.as.naming.NamingContext;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.util.NameParserResolver;
import org.jboss.dmr.Property;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service responsible for creating and managing the life-cycle of the Naming Server.
 *
 * @author John E. Bailey
 */
public class NamingService implements Service<NamingStore> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("naming");
    private final NamingStore namingStore;
    private NameParserResolver parserResolver;
    private Map<String, NameParser> resolverMappings;
    /**
     * Construct a new instance.
     *
     * @param namingStore The naming store.
     */
    public NamingService(final NamingStore namingStore, final String resolverClass, final List<Property> resolverMappings) throws Exception {
        this.namingStore = namingStore;
        this.parserResolver = (NameParserResolver) Class.forName(resolverClass).newInstance();
        this.resolverMappings = new HashMap<String,NameParser>(resolverMappings.size());
        for(Property p:resolverMappings){
            this.resolverMappings.put(p.getName(), (NameParser)Class.forName(p.getValue().asString()).newInstance());
        }
        this.resolverMappings = Collections.unmodifiableMap(this.resolverMappings);
    }

    /**
     * Creates a new NamingServer and sets the naming context to use the naming server.
     *
     * @param context The start context
     * @throws StartException If any errors occur setting up the naming server
     */
    public synchronized void start(StartContext context) throws StartException {
        ROOT_LOGGER.startingService();
        try {
            NamingContext.setActiveNamingStore(namingStore);
            NamingContext.setParserResolver(parserResolver);
            NamingContext.setParserMap(this.resolverMappings);
        } catch (Throwable t) {
            throw new StartException(MESSAGES.failedToStart("naming service"), t);
        }
    }

    /**
     * Removes the naming server from the naming context.
     *
     * @param context The stop context.
     */
    public synchronized void stop(StopContext context) {
        NamingContext.setActiveNamingStore(null);
        try {
            namingStore.close();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the naming store value.
     *
     * @return The naming store.
     * @throws IllegalStateException
     */
    public synchronized NamingStore getValue() throws IllegalStateException {
        return namingStore;
    }
}

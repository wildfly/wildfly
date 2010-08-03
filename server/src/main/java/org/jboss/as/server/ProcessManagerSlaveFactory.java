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

/**
 * 
 */
package org.jboss.as.server;

import org.jboss.as.process.ProcessManagerSlave;

/**
 * A ProcessManagerSlaveFactory.
 * 
 * @author Brian Stansberry
 */
public final class ProcessManagerSlaveFactory {

    private static final ProcessManagerSlaveFactory INSTANCE = new ProcessManagerSlaveFactory();
    
    public static ProcessManagerSlaveFactory getInstance() {
        return INSTANCE;
    }
    
    public ProcessManagerSlave getProcessManagerSlave(ServerEnvironment environment, MessageHandler handler) {
        
        // TODO JBAS-8259 -- possible socket-based communication
        // use environment to detect if PM wants that; use Standalone to
        // determine what socket to use
        
        // Problem: during primordial bootstrap we have no Standalone
        // Have ServerManager pass our address/port via command line?
        
        // For now, keep it simple
        return new ProcessManagerSlave(environment.getStdin(), environment.getStdout(), handler);
    }
    
    private ProcessManagerSlaveFactory() {}
}

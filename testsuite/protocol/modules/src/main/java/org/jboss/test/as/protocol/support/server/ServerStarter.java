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
package org.jboss.test.as.protocol.support.server;

import java.net.InetAddress;

import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.server.AbstractServer;
import org.jboss.as.server.Main;
import org.jboss.test.as.protocol.support.process.MockProcessManager;
import org.jboss.test.as.protocol.support.process.NoopExiter;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ServerStarter {


    public static AbstractServer createServer(String serverName, MockProcessManager pm, int smPort) throws Exception{
        String[] args = new String[] {
                CommandLineConstants.INTERPROCESS_NAME,
                serverName,
                CommandLineConstants.INTERPROCESS_PM_ADDRESS,
                InetAddress.getLocalHost().getHostAddress(),
                CommandLineConstants.INTERPROCESS_PM_PORT,
                String.valueOf(pm.getPort()),
                CommandLineConstants.INTERPROCESS_SM_ADDRESS,
                InetAddress.getLocalHost().getHostAddress(),
                CommandLineConstants.INTERPROCESS_SM_PORT,
                String.valueOf(smPort)
        };

        return Main.create(args, System.in, System.out, System.err, new NoopExiter());
    }

}

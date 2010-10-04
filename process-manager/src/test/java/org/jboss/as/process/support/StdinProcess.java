/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.process.support;

import java.io.DataInputStream;
import java.io.IOException;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class StdinProcess extends AbstractProcess {
    protected StdinProcess(String processName, int port) {
        super(processName, port);
    }

    public static void main(String[] args) {
        if (args.length < 1)
            System.exit(-1);
        StdinProcess process = new StdinProcess(args[0], getPort(args));

        String read = null;
        try {
            read = new DataInputStream(System.in).readUTF();
        } catch (IOException e) {
        }

        process.writeData(read);
        process.startThread();
    }

    @Override
    protected void shutdown() {
    }

    @Override
    protected void started() {
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerMaker {
    public Server makeServer() throws IOException {
        final List<String> args = new ArrayList<String>();
        if (false) {
            // Example: run at high priority on *NIX
            args.add("/usr/bin/nice");
            args.add("-n");
            args.add("-10");
        }
        if (false) {
            // Example: run only on processors 1-4 on Linux
            args.add("/usr/bin/taskset");
            args.add("0x0000000F");
        }
        args.add("/home/david/local/jdk/home/bin/java");
        args.add("-Djava.util.logging.manager=org.jboss.logmanager.LogManager");
        args.add("-jar");
        args.add("jboss-modules.jar");
        args.add("-mp");
        args.add("modules");
        args.add("org.jboss.as:server");
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.redirectErrorStream(false);
        final Process process = builder.start();

        // Read errors from here, pass to logger
        final InputStream errorStream = process.getErrorStream();
        // Read commands and responses from here
        final InputStream inputStream = process.getInputStream();
        // Write commands and responses to here
        final OutputStream outputStream = process.getOutputStream();
        return new Server(errorStream, inputStream, outputStream);
    }
}

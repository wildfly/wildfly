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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import org.jboss.stdio.LoggingOutputStream;
import org.jboss.stdio.NullInputStream;
import org.jboss.stdio.SimpleStdioContextSelector;
import org.jboss.stdio.StdioContext;

import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;

/**
 * The main-class entry point for server instances.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author John Bailey
 */
public final class Main {

    private Main() {
    }

    /**
     * The main method.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        
        // Grab copies of our streams.
        final InputStream in = System.in;
        final PrintStream out = System.out;
        final PrintStream err = System.err;

        // Install JBoss Stdio to avoid any nasty crosstalk.
        StdioContext.install();
        final StdioContext context = StdioContext.create(
            new NullInputStream(),
            new LoggingOutputStream(Logger.getLogger("stdout"), Level.INFO),
            new LoggingOutputStream(Logger.getLogger("stderr"), Level.ERROR)
        );
        StdioContext.setStdioContextSelector(new SimpleStdioContextSelector(context));

        boot(args);

        out.println("200 Server Ready");
        try {
            for (;;) {
                final String command = readCommand(in);
                if (command == null) break;
                out.println("000 Got command: " + command);
            }
        } catch (IOException e) {
            e.printStackTrace(err);
            System.exit(1);
        }
        System.exit(0);
    }

    private static void boot(final String[] args) {
        
        // TODO create a ServerEnvironment, construct a Server
    }

//    private static Server initializeServerConfig(final File configRoot, final String serverGroup, final String serverName) {
//        // Read serialized config and apply changes from log
//        return null;
//    }

    private static String readCommand(final InputStream in) throws IOException {
        final StringBuilder b = new StringBuilder();
        int c;
        while ((c = in.read()) != -1 && c != '\n') {
            b.append((char) (c & 0xff));
        }
        if (b.length() == 0 && c == -1) {
            return null;
        }
        return b.toString();
    }
}

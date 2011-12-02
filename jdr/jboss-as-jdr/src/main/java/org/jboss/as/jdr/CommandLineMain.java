 /*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jdr;

import org.jboss.as.controller.OperationFailedException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.HelpFormatter;

 /**
 * Provides a main for collecting a JDR report from the command line.
 *
 * @author Mike M. Clark
 * @author Jesse Jaggars
 */
public class CommandLineMain {

    private static CommandLineParser parser = new GnuParser();
    private static Options options = new Options();
    private static HelpFormatter formatter = new HelpFormatter();
    private static final String usage = "jdr.{sh,bat} [options]";

    static {
        options.addOption("H", "help", false, "prints help and exits");
        options.addOption("h", "host", true, "hostname that the management api is bound to. (default: localhost)");
        options.addOption("p", "port", true, "port that the management api is bound to. (default: 9990)");
    }

    /**
     * Creates a JBoss Diagnostic Reporter (JDR) Report. A JDR report response
     * is printed to <code>System.out</code>.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        String port = "9990";
        String host = "localhost";

        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("help")) {
                formatter.printHelp(usage, options);
                return;
            }
            if (line.hasOption("host")) {
                host = line.getOptionValue("host");
            }

            if (line.hasOption("port")) {
                port = line.getOptionValue("port");
            }
        } catch (Exception e) {
            formatter.printHelp(usage, options);
            return;
        }

        System.out.println("Initializing JBoss Diagnostic Reporter...");

        JdrReportService reportService = new JdrReportService();

        JdrReport response = null;
        try {
            response = reportService.standaloneCollect(host, port);
        } catch (OperationFailedException e) {
            System.out.println("Failed to complete the JDR report: " + e.getMessage());
        }

        System.out.println("JDR started: " + response.getStartTime().toString());
        System.out.println("JDR ended: " + response.getEndTime().toString());
        System.out.println("JDR location: " + response.getLocation());
    }
}

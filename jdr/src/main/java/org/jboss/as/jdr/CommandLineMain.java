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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jboss.as.cli.scriptsupport.CLI;
import org.jboss.as.cli.scriptsupport.CLI.Result;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.jdr.logger.JdrLogger;
import org.jboss.dmr.ModelNode;

/**
 * Provides a main for collecting a JDR report from the command line.
 *
 * @author Mike M. Clark
 * @author Jesse Jaggars
 */
public class CommandLineMain {

    private static final CommandLineParser parser = new GnuParser();
    private static final Options options = new Options();
    private static final HelpFormatter formatter = new HelpFormatter();
    private static final String usage = "jdr.{sh,bat,ps1} [options]";
    private static final String NEW_LINE = String.format("%n");

    static {
        options.addOption("h", "help", false, JdrLogger.ROOT_LOGGER.jdrHelpMessage());
        options.addOption("H", "host", true, JdrLogger.ROOT_LOGGER.jdrHostnameMessage());
        options.addOption("p", "port", true, JdrLogger.ROOT_LOGGER.jdrPortMessage());
        options.addOption("s", "protocol", true, JdrLogger.ROOT_LOGGER.jdrProtocolMessage());
        options.addOption("c", "config", true, JdrLogger.ROOT_LOGGER.jdrConfigMessage());
    }

    /**
     * Creates a JBoss Diagnostic Reporter (JDR) Report. A JDR report response
     * is printed to <code>System.out</code>.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        int port = 9990;
        String host = "localhost";
        String protocol = "remote+http";
        String config = null;
        try {
            CommandLine line = parser.parse(options, args, false);

            if (line.hasOption("help")) {
                formatter.printHelp(usage, NEW_LINE + JdrLogger.ROOT_LOGGER.jdrDescriptionMessage(), options, null);
                return;
            }
            if (line.hasOption("host")) {
                host = line.getOptionValue("host");
            }

            if (line.hasOption("port")) {
                port = Integer.parseInt(line.getOptionValue("port"));
            }

            if (line.hasOption("protocol")) {
                protocol = line.getOptionValue("protocol");
            }

            if (line.hasOption("config")) {
                config = line.getOptionValue("config");
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp(usage, options);
            return;
        } catch (NumberFormatException nfe) {
            System.out.println(nfe.getMessage());
            formatter.printHelp(usage, options);
            return;
        }

        System.out.println("Initializing JBoss Diagnostic Reporter...");

        // Try to run JDR on the Wildfly JVM
        CLI cli = CLI.newInstance();
        boolean embedded = false;
        JdrReport report = null;
        try {
            System.out.println(String.format("Trying to connect to %s %s:%s", protocol, host, port));
            cli.connect(protocol, host, port, null, null);
        } catch (IllegalStateException ex) {
            System.out.println("Starting embedded server");
            String startEmbeddedServer = "embed-server --std-out=echo " + ((config != null && ! config.isEmpty()) ? (" --server-config=" + config) : "");
            cli.getCommandContext().handleSafe(startEmbeddedServer);
            embedded = true;
        }
        try {
            Result cmdResult = cli.cmd("/subsystem=jdr:generate-jdr-report()");
            ModelNode response = cmdResult.getResponse();
            if(Operations.isSuccessfulOutcome(response) || !embedded) {
                reportFailure(response);
                ModelNode result = response.get(ClientConstants.RESULT);
                report = new JdrReport(result);
            } else {
                report = standaloneCollect(cli, protocol, host, port);
            }
        } catch(IllegalStateException ise) {
            System.out.println(ise.getMessage());
            report = standaloneCollect(cli, protocol, host, port);
        } finally {
            if(cli != null) {
                try {
                    if(embedded)
                        cli.getCommandContext().handleSafe("stop-embedded-server");
                    else
                        cli.disconnect();
                } catch(Exception e) {
                    System.out.println("Caught exception while disconnecting: " + e.getMessage());
                }
            }
        }
        printJdrReportInfo(report);
        System.exit(0);
    }

    private static void printJdrReportInfo(JdrReport report) {
        if(report != null) {
            System.out.println("JDR started: " + report.getFormattedStartTime());
            System.out.println("JDR ended: " + report.getFormattedEndTime());
            System.out.println("JDR location: " + report.getLocation());
            System.out.flush();
        }
    }

    private static JdrReport standaloneCollect(CLI cli, String protocol, String host, int port) {
        // Unable to connect to a running server, so proceed without it
        JdrReportService reportService = new JdrReportService();
        JdrReport report = null;
        try {
            report = reportService.standaloneCollect(cli, protocol, host, port);
        } catch (OperationFailedException e) {
            System.out.println("Failed to complete the JDR report: " + e.getMessage());
        }
        return report;
    }

    private static void reportFailure(final ModelNode node) {
        if (!node.get(ClientConstants.OUTCOME).asString().equals(ClientConstants.SUCCESS)) {
            final String msg;
            if (node.hasDefined(ClientConstants.FAILURE_DESCRIPTION)) {
                if (node.hasDefined(ClientConstants.OP)) {
                    msg = String.format("Operation '%s' at address '%s' failed: %s", node.get(ClientConstants.OP), node.get(ClientConstants.OP_ADDR), node.get(ClientConstants.FAILURE_DESCRIPTION));
                } else {
                    msg = String.format("Operation failed: %s", node.get(ClientConstants.FAILURE_DESCRIPTION));
                }
            } else {
                msg = String.format("Operation failed: %s", node);
            }
            throw new RuntimeException(msg);
        }
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.version;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Usage {

    private static final String NEW_LINE = String.format("%n");

    private final List<List<String>> arguments = new ArrayList<List<String>>();
    private final List<String> instructions = new ArrayList<String>();

    public String getDefaultUsageHeadline(String executableBaseName) {
        boolean isWindows = (getSystemProperty("os.name")).toLowerCase(Locale.ENGLISH).contains("windows");
        String executableName = isWindows ? executableBaseName : executableBaseName + ".sh";
        return UsageMessages.MESSAGES.argUsage(executableName);
    }

    public void addArguments(String... args){
        ArrayList<String> tempArguments = new ArrayList<String>();
        for (String arg : args) {
            tempArguments.add(arg);
        }
        arguments.add(tempArguments);
    }

    public void addInstruction(String instruction){
        instructions.add(instruction);
    }

    public String usage(String headline) {

        final StringBuilder sb = new StringBuilder();
        sb.append(NEW_LINE).append(headline).append(NEW_LINE);

        for (int i = 0; i < arguments.size(); i++) {
            sb.append(getCommand(i)).append(NEW_LINE);
        }
        return sb.toString();
    }

    private String getCommand(int i){
        // Segment Instructions
        final List<String> segmentedInstructions = new ArrayList<String>();
        segmentInstructions(instructions.get(i), segmentedInstructions);

        // Segment Arguments
        final List<String> segmentedArguments = new ArrayList<String>();
        segmentArguments(arguments.get(i), segmentedArguments, 0);

        // First line
        StringBuilder output = new StringBuilder(String.format("    %-35s %s", segmentedArguments.remove(0), segmentedInstructions.remove(0)));
        output.append(NEW_LINE);

        if (segmentedArguments.size() <= segmentedInstructions.size()) {
            int count = 0;
            for (String arg : segmentedArguments) {
                output.append(String.format("         %-30s %s", arg, segmentedInstructions.remove(count)));
                output.append(NEW_LINE);
                count++;
            }

            for (String instruction : segmentedInstructions) {
                output.append(String.format("%-40s%s", " ", instruction));
                output.append(NEW_LINE);
            }
        } else {
            int count = 0;
            for (String instruction : segmentedInstructions ) {
                output.append(String.format("         %-30s %s", segmentedArguments.remove(count), instruction));
                output.append(NEW_LINE);
                count++;
            }

            for (String arg : segmentedArguments ) {
                output.append(String.format("         %-30s", arg));
                output.append(NEW_LINE);
            }
        }

        output.append(NEW_LINE);
        return output.toString();
    }

    private static void segmentArguments(List<String> input, List<String> output, int depth) {
        int width = depth == 0 ? 35 : 30;

        if (input.size() > 0) {
            StringBuilder argumentsString = new StringBuilder();
            for (int i = 0; i < input.size();) {
                // Trim in case an argument is too large for the width. Shouldn't happen.
                if (input.get(0).length() > width) {
                    String tooLong = input.remove(0);
                    tooLong.substring(0, width-5);
                    input.add("Command removed. Too long.");
                }

                if (input.size() == 1 && (argumentsString.toString().length() + input.get(0).length() <= width)) {
                    argumentsString.append(input.remove(0));
                } else if (argumentsString.toString().length() + input.get(0).length() + 2 <= width ) {
                    argumentsString.append(input.remove(0) + ", ");
                } else {
                   break;
                }

            }
            output.add(argumentsString.toString());
            segmentArguments(input, output, depth+1);
        }
    }

    private static void segmentInstructions(String instructions, List<String> segments) {
        if (instructions.length() <= 40) {
            segments.add(instructions);
        } else {
            String testFragment = instructions.substring(0,40);
            int lastSpace = testFragment.lastIndexOf(' ');
            if (lastSpace < 0) {
                // degenerate case; we just have to chop not at a space
                lastSpace = 39;
            }
            segments.add(instructions.substring(0, lastSpace + 1));
            segmentInstructions(instructions.substring(lastSpace + 1), segments);
        }
    }

    private static String getSystemProperty(final String key) {
        if (System.getSecurityManager() == null) {
            return System.getProperty(key);
        }

        return AccessController.doPrivileged(new PrivilegedAction<String>() {

            @Override
            public String run() {
                return System.getProperty(key);
            }
        });
    }

}

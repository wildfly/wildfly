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

package org.jboss.as.patching.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.UUID;

import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.PatchMessages;

/**
 * Generate a simple template for a give patch type.
 *
 * @author Emanuel Muckenhuber
 */
class TemplateGenerator {

    private static final String TAB = "   ";
    private static final String LF = "\r\n";

    static String generate(final String... args) throws IOException {

        boolean stdout = false;
        Boolean oneOff = null;
        String patchID = UUID.randomUUID().toString();
        String appliesToVersion = null;

        final int argsLength = args.length;
        for (int i = 0; i < argsLength; i++) {
            final String arg = args[i];
            try {
                if ("--help".equals(arg) || "-h".equals(arg) || "-H".equals(arg)) {
                    usage();
                    return null;
                } else if(arg.equals("--one-off")) {
                    if (oneOff == null) {
                        oneOff = Boolean.TRUE;
                        patchID = args[++i];
                    } else {
                        usage();
                        return null;
                    }
                } else if(arg.equals("--cumulative")) {
                    if (oneOff == null) {
                        oneOff = Boolean.FALSE;
                        patchID = args[++i];
                    } else {
                        usage();
                        return null;
                    }
                } else if(arg.equals("--applies-to-version")) {
                    appliesToVersion = args[++i];
                } else if(arg.equals("--std.out")) {
                    stdout = true;
                } else if (arg.equals("--create-template")) {
                    continue;
                } else {
                    System.err.println(PatchMessages.MESSAGES.argumentExpected(arg));
                    usage();
                    return null;
                }
            } catch (IndexOutOfBoundsException e) {
                System.err.println(PatchMessages.MESSAGES.argumentExpected(arg));
                usage();
                return null;
            }
        }

        if (oneOff == null) {
            usage();
            return null;
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("<?xml version='1.0' encoding='UTF-8'?>").append(LF);
        builder.append("<patch-config xmlns=\"urn:jboss:patch-config:1.0\">").append(LF);
        builder.append(TAB).append("<name>").append(patchID).append("</name>").append(LF);
        builder.append(TAB).append("<description>No description available</description>").append(LF);
        builder.append(TAB);
        if (oneOff) {
            builder.append("<one-off ");
        } else {
            builder.append("<cumulative ");
        }
        if (appliesToVersion != null) {
            builder.append("applies-to-version=\"").append(appliesToVersion).append("\"");
        }
        builder.append(" />").append(LF);

        // Write patch element
        builder.append(TAB).append("<element patch-id=\"").append("layer-base-").append(patchID).append("\">").append(LF);
        builder.append(TAB).append(TAB);
        if (oneOff) {
            builder.append("<one-off ");
        } else {
            builder.append("<cumulative ");
        }
        builder.append("name=\"base\" />").append(LF);
        builder.append(TAB).append(TAB).append("<description>No description available</description>").append(LF);

        if (oneOff) {
            builder.append(TAB).append(TAB).append("<specified-content>").append(LF);
            builder.append(TAB).append(TAB).append(TAB).append("<modules>").append(LF);
            builder.append(TAB).append(TAB).append(TAB).append(TAB).append("<updated name=\"org.jboss.as.server\" />").append(LF);
            builder.append(TAB).append(TAB).append(TAB).append("</modules>").append(LF);
            builder.append(TAB).append(TAB).append("</specified-content>").append(LF);
        }
        builder.append(TAB).append("</element>").append(LF);

        if (oneOff) {
            builder.append(TAB).append("<specified-content>").append(LF);
            builder.append(TAB).append(TAB).append("<misc-files>").append(LF);
            builder.append(TAB).append(TAB).append(TAB).append("<updated path=\"version.txt\" />").append(LF);
            builder.append(TAB).append(TAB).append("</misc-files>").append(LF);
            builder.append(TAB).append("</specified-content>").append(LF);
        } else {
            builder.append(TAB).append("<generate-by-diff />").append(LF);
        }

        builder.append("</patch-config>").append(LF);

        final String output = builder.toString();
        if (stdout) {
            System.out.println(output);
        } else {
            final File file = new File("patch-config-" +patchID + ".xml");
            final Writer writer = new FileWriter(file);
            try {
                writer.write(output);
                writer.close();
            } finally {
                IoUtils.safeClose(writer);
            }
        }
        return output;
    }

    static void usage() {
        final StringBuilder builder = new StringBuilder();
        builder.append("USAGE:").append(LF);
        builder.append("patch-gen.sh --create-template --one-of     [patch-id]").append(LF);
        builder.append("patch-gen.sh --create-template --cumulative [patch-id]").append(LF);
        builder.append(LF);
        builder.append("this will create a patch-config-[patch-id].xml").append(LF);
        builder.append("if this is not desired just append --std.out").append(LF);
        System.err.println(builder.toString());
    }

}

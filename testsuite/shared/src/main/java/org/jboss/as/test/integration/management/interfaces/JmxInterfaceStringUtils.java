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

package org.jboss.as.test.integration.management.interfaces;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.dmr.ModelNode;

/**
 * @author jcechace
 * @author Ladislav Thon <lthon@redhat.com>
 */
final class JmxInterfaceStringUtils {
    private JmxInterfaceStringUtils() {}

    static String rawString(String message) {
        String raw = removeQuotes(message);
        // This is need as StringModelValue#toString() returns escaped output
        return removeEscapes(raw);
    }

    private static String removeQuotes(String string) {
        if (string.startsWith("\"") && string.endsWith("\"")) {
            string = string.substring(1, string.length() - 1);
        }
        return string;
    }

    private static String removeEscapes(String string) {
        Pattern pattern = Pattern.compile("\\\\(.)");
        Matcher matcher = pattern.matcher(string);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String followup = matcher.group();
            matcher.appendReplacement(result, followup);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    static String toDashCase(String string) {
        String regex = "([a-z])([A-Z])";
        String replacement = "$1-$2";
        return string.replaceAll(regex, replacement).toLowerCase();
    }

    static String toCamelCase(String str) {
        Pattern pattern = Pattern.compile("-([a-z])");
        Matcher matcher = pattern.matcher(str);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String upperCaseLetter = matcher.group(1).toUpperCase();
            matcher.appendReplacement(result, upperCaseLetter);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    static ModelNode nodeFromString(String string) {
        ModelNode result;
        try {
            result = ModelNode.fromString(string);
        } catch (Exception e) {
            try {
                result = ModelNode.fromJSONString(string);
            } catch (Exception e1) {
                result = new ModelNode(string);
            }
        }
        return result;
    }
}

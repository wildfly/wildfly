/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jmx.model;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import org.jboss.as.controller.PathElement;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class NameConverter {

    public static String createValidAddOperationName(PathElement childElement) {
        return createValidName(ADD, childElement.getKey(), childElement.getValue());
    }

    public static String convertToCamelCase(String word) {
        StringBuilder sb = new StringBuilder();
        appendCamelCaseWord(sb, true, word.split("-"));
        return sb.toString();
    }

    public static String convertFromCamelCase(String word) {
        StringBuilder sb = new StringBuilder();
        for (char ch : word.toCharArray()) {
            if (Character.isLowerCase(ch)) {
                sb.append(ch);
            } else {
                sb.append("-");
                sb.append(Character.toLowerCase(ch));
            }
        }
        return sb.toString();
    }

    private static String createValidName(String...parts) {
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1 ; i < parts.length ; i++) {
            if (parts[i].equals("*")) {
                continue;
            }
            appendCamelCaseWord(sb, false, parts[i].split("-"));
        }
        return sb.toString();
    }

    private static void appendCamelCaseWord(StringBuilder sb, boolean isStart, String...parts) {
        if (parts.length == 1) {
            if (!isStart) {
                sb.append(Character.toUpperCase(parts[0].charAt(0)));
                sb.append(parts[0].substring(1));
            } else {
                sb.append(parts[0]);
            }
        } else {
            for (int i = 0 ; i < parts.length ; i++) {
                final boolean isCurrentStart = isStart && i == 0;
                appendCamelCaseWord(sb, isCurrentStart, parts[i]);
            }
        }
    }
}

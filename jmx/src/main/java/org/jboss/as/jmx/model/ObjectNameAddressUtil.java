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

import static org.jboss.as.jmx.JmxMessages.MESSAGES;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;

/**
 * Utility class to convert between PathAddress and ObjectName
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class ObjectNameAddressUtil {

    private static final EscapedCharacter[] ESCAPED_KEY_CHARACTERS;
    static {
        List<EscapedCharacter> keys = new ArrayList<EscapedCharacter>();

        //From ObjectName javadoc:
        //Each key is a nonempty string of characters which may not contain any of the characters
        //comma (,), equals (=), colon, asterisk, or question mark. The same key may not occur twice in a given ObjectName.
        keys.add(new EscapedCharacter('*'));
        keys.add(new EscapedCharacter('?'));
        keys.add(new EscapedCharacter(':'));
        keys.add(new EscapedCharacter('='));
        keys.add(new EscapedCharacter(','));

        ESCAPED_KEY_CHARACTERS = keys.toArray(new EscapedCharacter[keys.size()]);
    }

    static ObjectName createObjectName(final PathAddress pathAddress) {

        if (pathAddress.size() == 0) {
            return Constants.ROOT_MODEL_NAME;
        }
        final StringBuilder sb = new StringBuilder(Constants.DOMAIN);
        sb.append(":");
        boolean first = true;
        for (PathElement element : pathAddress) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            escapeKey(ESCAPED_KEY_CHARACTERS, sb, element.getKey());
            sb.append("=");
            escapeValue(sb, element.getValue());
        }
        try {
            return ObjectName.getInstance(sb.toString());
        } catch (MalformedObjectNameException e) {
            throw MESSAGES.cannotCreateObjectName(e, pathAddress);
        }
    }

    /**
     * Converts the ObjectName to a PathAddress.
     *
     * @param name the ObjectName
     * @return the PathAddress if it exists in the model, {@code null} otherwise
     */
    static PathAddress resolvePathAddress(final Resource rootResource, final ObjectName name) {
        if (!name.getDomain().equals(Constants.DOMAIN)) {
            return null;
        }
        if (name.equals(Constants.ROOT_MODEL_NAME)) {
            return PathAddress.EMPTY_ADDRESS;
        }
        Hashtable<String, String> properties = name.getKeyPropertyList();
        return searchPathAddress(PathAddress.EMPTY_ADDRESS, rootResource, properties);
    }

    private static PathAddress searchPathAddress(final PathAddress address, final Resource resource, final Map<String, String> properties) {
        if (properties.size() == 0) {
            return address;
        }
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            PathElement childElement = PathElement.pathElement(
                    replaceEscapedCharactersInKey(entry.getKey()),
                    replaceEscapedCharactersInValue(entry.getValue()));
            Resource child = resource.getChild(childElement);
            if (child != null) {
                Map<String, String> childProps = new HashMap<String, String>(properties);
                childProps.remove(entry.getKey());
                PathAddress foundAddr = searchPathAddress(address.append(childElement), child, childProps);
                if (foundAddr != null) {
                    return foundAddr;
                }
            }
        }
        return null;
    }

    static boolean isReservedDomain(ObjectName name) {
        return name.getDomain().equals(Constants.DOMAIN);
    }

    private static void escapeKey(EscapedCharacter[] escapedCharacters, StringBuilder sb, String value) {
        for (EscapedCharacter escapedCharacter : escapedCharacters) {
            value = value.replace(escapedCharacter.getChar().toString(), escapedCharacter.getEscaped());
        }
        sb.append(value);
    }

    private static void escapeValue(final StringBuilder sb, final String value) {
        final boolean containsAsterix = value.contains("*");
        final boolean containsBackslash = value.contains("\\");
        final boolean containsColon = value.contains(":");
        final boolean containsEquals = value.contains("=");
        final boolean containsNewLine = value.contains("\n");
        final boolean containsQuestionMark = value.contains("?");
        final boolean containsQuote = value.contains("\"");

        boolean quoted = containsAsterix || containsBackslash || containsColon || containsEquals || containsNewLine || containsQuestionMark || containsQuote;
        if (quoted) {
            String replaced = value;
            sb.append("\"");

            replaced = checkAndReplace(containsAsterix, replaced, "*", "\\*");
            replaced = checkAndReplace(containsBackslash, replaced, "\\", "\\\\");
            //colon and equals do not need escaping
            replaced = checkAndReplace(containsNewLine, replaced, "\n", "\\n");
            replaced = checkAndReplace(containsQuestionMark, replaced, "?", "\\?");
            replaced = checkAndReplace(containsQuote, replaced, "\"", "\\\"");
            sb.append(replaced);

            sb.append("\"");
        } else {
            sb.append(value);
        }
    }

    private static String checkAndReplace(boolean condition, String original, String search, String replacement) {
        if (condition) {
            return original.replace(search, replacement);
        }
        return original;
    }

    private static String replaceEscapedCharactersInKey(String escaped) {
        if (escaped.contains("%x")) {
            for (EscapedCharacter escapedCharacter : ESCAPED_KEY_CHARACTERS) {
                escaped = escaped.replace(escapedCharacter.getEscaped(), escapedCharacter.getChar());
            }
        }
        return escaped;
    }

    private static String replaceEscapedCharactersInValue(final String escaped) {
        if (escaped.startsWith("\"") && escaped.endsWith("\"")) {
            final boolean containsAsterix = escaped.contains("\\*");
            final boolean containsBackslash = escaped.contains("\\\\");
            final boolean containsNewLine = escaped.contains("\\n");
            final boolean containsQuestionMark = escaped.contains("\\?");
            final boolean containsQuote = escaped.contains("\\\"");

            String replaced = escaped.substring(1, escaped.length() - 1);
            replaced = checkAndReplace(containsAsterix, replaced, "\\*", "*");
            replaced = checkAndReplace(containsBackslash, replaced, "\\\\", "\\");
            replaced = checkAndReplace(containsNewLine, replaced, "\\n", "\n");
            replaced = checkAndReplace(containsQuestionMark, replaced, "\\?", "?");
            replaced = checkAndReplace(containsQuote, replaced, "\\\"", "\"");
            return replaced;
        } else {
            return escaped;
        }
    }

    private static class EscapedCharacter {
        private final String ch;
        private final String hexPart;
        private final String escaped;

        EscapedCharacter(Character ch){
            this.ch = String.valueOf(ch);
            hexPart = Integer.toHexString(ch);
            escaped = "%x" + hexPart;
        }

        String getChar() {
            return ch;
        }

        String getEscaped() {
            return escaped;
        }
    }
}

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
    private static final EscapedCharacter[] ESCAPED_VALUE_CHARACTERS;
    static {
        List<EscapedCharacter> keys = new ArrayList<EscapedCharacter>();
        List<EscapedCharacter> values = new ArrayList<EscapedCharacter>();

        keys.add(new EscapedCharacter('*'));
        keys.add(new EscapedCharacter('?'));
        keys.add(new EscapedCharacter('+'));
        keys.add(new EscapedCharacter(':'));
        keys.add(new EscapedCharacter('='));
        keys.add(new EscapedCharacter(','));

        values.add(new EscapedCharacter(':'));
        values.add(new EscapedCharacter('='));
        values.add(new EscapedCharacter(','));

        ESCAPED_KEY_CHARACTERS = keys.toArray(new EscapedCharacter[keys.size()]);
        ESCAPED_VALUE_CHARACTERS = keys.toArray(new EscapedCharacter[values.size()]);
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
            appendEscapedCharacter(ESCAPED_KEY_CHARACTERS, sb, element.getKey());
            sb.append("=");
            appendEscapedCharacter(ESCAPED_VALUE_CHARACTERS, sb, element.getValue());
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
     * @return the PathAddress if it exists in the model, {@code false} otherwise
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
                    replaceEscapedCharacters(entry.getKey()),
                    replaceEscapedCharacters(entry.getValue()));
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

    private static void appendEscapedCharacter(EscapedCharacter[] escapedCharacters, StringBuilder sb, String value) {
        for (EscapedCharacter escapedCharacter : escapedCharacters) {
            value = value.replace(escapedCharacter.getChar().toString(), escapedCharacter.getEscaped());
        }
        sb.append(value);
    }

    private static String replaceEscapedCharacters(String escaped) {
        if (escaped.contains("%x")) {
            for (EscapedCharacter escapedCharacter : ESCAPED_KEY_CHARACTERS) {
                escaped = escaped.replace(escapedCharacter.getEscaped(), escapedCharacter.getChar());
            }
        }
        return escaped;
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

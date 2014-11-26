/*
 * Copyright (c) 1999, 2003, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.wildfly.iiop.openjdk.naming.jndi;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import javax.naming.CompositeName;
import javax.naming.CompoundName;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

import org.omg.CosNaming.NameComponent;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * Parsing routines for NameParser as well as COS Naming stringified names.
 * This is used by CNCtx to create a NameComponent[] object and vice versa.
 * It follows Section 4.5 of Interoperable Naming Service (INS) 98-10-11.
 * In summary, the stringified form is a left-to-right, forward-slash
 * separated name. id and kinds are separated by '.'. backslash is the
 * escape character.
 *
 * @author Rosanna Lee
 */

public final class CNNameParser implements NameParser {

    private static final Properties mySyntax = new Properties();
    private static final char kindSeparator = '.';
    private static final char compSeparator = '/';
    private static final char escapeChar = '\\';

    static {
        mySyntax.put("jndi.syntax.direction", "left_to_right");
        mySyntax.put("jndi.syntax.separator", "" + compSeparator);
        mySyntax.put("jndi.syntax.escape", "" + escapeChar);
    }

    ;

    /**
     * Constructs a new name parser for parsing names in INS syntax.
     */
    public CNNameParser() {
    }

    /**
     * Returns a CompoundName given a string in INS syntax.
     *
     * @param name The non-null string representation of the name.
     * @return a non-null CompoundName
     */
    public Name parse(String name) throws NamingException {
        Vector comps = insStringToStringifiedComps(name);
        return new CNCompoundName(comps.elements());
    }

    /**
     * Creates a NameComponent[] from a Name structure.
     * Used by CNCtx to convert the input Name arg into a NameComponent[].
     *
     * @param name a CompoundName or a CompositeName;
     *          each component must be the stringified form of a NameComponent.
     */
    static NameComponent[] nameToCosName(Name name)
            throws InvalidNameException {
        int len = name.size();
        if (len == 0) {
            return new NameComponent[0];
        }

        NameComponent[] answer = new NameComponent[len];
        for (int i = 0; i < len; i++) {
            answer[i] = parseComponent(name.get(i));
        }
        return answer;
    }

    /**
     * Returns the INS stringified form of a NameComponent[].
     * Used by CNCtx.getNameInNamespace(), CNCompoundName.toString().
     */
    static String cosNameToInsString(NameComponent[] cname) {
        StringBuffer str = new StringBuffer();
        for (int i = 0; i < cname.length; i++) {
            if (i > 0) {
                str.append(compSeparator);
            }
            str.append(stringifyComponent(cname[i]));
        }
        return str.toString();
    }

    /**
     * Creates a CompositeName from a NameComponent[].
     * Used by ExceptionMapper and CNBindingEnumeration to convert
     * a NameComponent[] into a composite name.
     */
    static Name cosNameToName(NameComponent[] cname) {
        Name nm = new CompositeName();
        for (int i = 0; cname != null && i < cname.length; i++) {
            try {
                nm.add(stringifyComponent(cname[i]));
            } catch (InvalidNameException e) {
                // ignore
            }
        }
        return nm;
    }

    /**
     * Converts an INS-syntax string name into a Vector in which
     * each element of the vector contains a stringified form of
     * a NameComponent.
     */
    private static Vector insStringToStringifiedComps(String str)
            throws InvalidNameException {

        int len = str.length();
        Vector components = new Vector(10);
        char[] id = new char[len];
        char[] kind = new char[len];
        int idCount, kindCount;
        boolean idMode;
        for (int i = 0; i < len; ) {
            idCount = kindCount = 0; // reset for new component
            idMode = true;           // always start off parsing id
            while (i < len) {
                if (str.charAt(i) == compSeparator) {
                    break;

                } else if (str.charAt(i) == escapeChar) {
                    if (i + 1 >= len) {
                        throw IIOPLogger.ROOT_LOGGER.unescapedCharacter(str);
                    } else if (isMeta(str.charAt(i + 1))) {
                        ++i; // skip escape and let meta through
                        if (idMode) {
                            id[idCount++] = str.charAt(i++);
                        } else {
                            kind[kindCount++] = str.charAt(i++);
                        }
                    } else {
                        throw IIOPLogger.ROOT_LOGGER.invalidEscapedCharacter(str);
                    }

                } else if (idMode && str.charAt(i) == kindSeparator) {
                    // just look for the first kindSeparator
                    ++i; // skip kind separator
                    idMode = false;

                } else {
                    if (idMode) {
                        id[idCount++] = str.charAt(i++);
                    } else {
                        kind[kindCount++] = str.charAt(i++);
                    }
                }
            }
            components.addElement(stringifyComponent(
                    new NameComponent(new String(id, 0, idCount),
                            new String(kind, 0, kindCount))));

            if (i < len) {
                ++i; // skip separator
            }
        }

        return components;
    }

    /**
     * Return a NameComponent given its stringified form.
     */
    private static NameComponent parseComponent(String compStr)
            throws InvalidNameException {
        NameComponent comp = new NameComponent();
        int kindSep = -1;
        int len = compStr.length();

        int j = 0;
        char[] newStr = new char[len];
        boolean escaped = false;

        // Find the kind separator
        for (int i = 0; i < len && kindSep < 0; i++) {
            if (escaped) {
                newStr[j++] = compStr.charAt(i);
                escaped = false;
            } else if (compStr.charAt(i) == escapeChar) {
                if (i + 1 >= len) {
                    throw IIOPLogger.ROOT_LOGGER.unescapedCharacter(compStr);
                } else if (isMeta(compStr.charAt(i + 1))) {
                    escaped = true;
                } else {
                    throw IIOPLogger.ROOT_LOGGER.invalidEscapedCharacter(compStr);
                }
            } else if (compStr.charAt(i) == kindSeparator) {
                kindSep = i;
            } else {
                newStr[j++] = compStr.charAt(i);
            }
        }

        // Set id
        comp.id = new String(newStr, 0, j);

        // Set kind
        if (kindSep < 0) {
            comp.kind = "";  // no kind separator
        } else {
            // unescape kind
            j = 0;
            escaped = false;
            for (int i = kindSep + 1; i < len; i++) {
                if (escaped) {
                    newStr[j++] = compStr.charAt(i);
                    escaped = false;
                } else if (compStr.charAt(i) == escapeChar) {
                    if (i + 1 >= len) {
                        throw IIOPLogger.ROOT_LOGGER.unescapedCharacter(compStr);
                    } else if (isMeta(compStr.charAt(i + 1))) {
                        escaped = true;
                    } else {
                        throw IIOPLogger.ROOT_LOGGER.invalidEscapedCharacter(compStr);
                    }
                } else {
                    newStr[j++] = compStr.charAt(i);
                }
            }
            comp.kind = new String(newStr, 0, j);
        }
        return comp;
    }

    private static String stringifyComponent(NameComponent comp) {
        StringBuffer one = new StringBuffer(escape(comp.id));
        if (comp.kind != null && !comp.kind.equals("")) {
            one.append(kindSeparator + escape(comp.kind));
        }
        if (one.length() == 0) {
            return "" + kindSeparator;  // if neither id nor kind specified
        } else {
            return one.toString();
        }
    }

    /**
     * Returns a string with '.', '\', '/' escaped. Used when
     * stringifying the name into its INS stringified form.
     */
    private static String escape(String str) {
        if (str.indexOf(kindSeparator) < 0 &&
                str.indexOf(compSeparator) < 0 &&
                str.indexOf(escapeChar) < 0) {
            return str;                         // no meta characters to escape
        } else {
            int len = str.length();
            int j = 0;
            char[] newStr = new char[len + len];
            for (int i = 0; i < len; i++) {
                if (isMeta(str.charAt(i))) {
                    newStr[j++] = escapeChar;   // escape meta character
                }
                newStr[j++] = str.charAt(i);
            }
            return new String(newStr, 0, j);
        }
    }

    /**
     * In INS, there are three meta characters: '.', '/' and '\'.
     */
    private static boolean isMeta(char ch) {
        switch (ch) {
            case kindSeparator:
            case compSeparator:
            case escapeChar:
                return true;
        }
        return false;
    }

    /**
     * An implementation of CompoundName that bypasses the parsing
     * and stringifying code of the default CompoundName.
     */
    static final class CNCompoundName extends CompoundName {
        CNCompoundName(Enumeration enum_) {
            super(enum_, CNNameParser.mySyntax);
        }

        public Object clone() {
            return new CNCompoundName(getAll());
        }

        public Name getPrefix(int posn) {
            Enumeration comps = super.getPrefix(posn).getAll();
            return new CNCompoundName(comps);
        }

        public Name getSuffix(int posn) {
            Enumeration comps = super.getSuffix(posn).getAll();
            return new CNCompoundName(comps);
        }

        public String toString() {
            try {
                // Convert Name to NameComponent[] then stringify
                return cosNameToInsString(nameToCosName(this));
            } catch (InvalidNameException e) {
                return super.toString();
            }
        }

        private static final long serialVersionUID = -6599252802678482317L;
    }
}

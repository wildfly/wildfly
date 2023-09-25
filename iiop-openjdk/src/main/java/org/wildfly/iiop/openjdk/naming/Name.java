/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk.naming;

import java.util.Vector;
import org.omg.CORBA.INTERNAL;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextPackage.InvalidName;

/**
 * A convenience class for names and converting between Names and their string representation
 *
 * @author Gerald Brose, FU Berlin
 */

public class Name implements java.io.Serializable {
    private NameComponent[] fullName;
    private NameComponent baseName;

    /** context part of this Name */
    private NameComponent[] ctxName;

    public Name() {
        fullName = null;
        baseName = null;
        ctxName = null;
    }

    /**
     * create a name from an array of NameComponents
     */

    public Name(NameComponent[] n) throws InvalidName {
        if (n == null || n.length == 0)
            throw new InvalidName();

        fullName = n;
        baseName = n[n.length - 1];
        if (n.length > 1) {
            ctxName = new NameComponent[n.length - 1];
            for (int i = 0; i < n.length - 1; i++)
                ctxName[i] = n[i];
        } else
            ctxName = null;
    }

    /**
     * create a name from a stringified name
     */

    public Name(String string_name) throws InvalidName {
        this(toName(string_name));
    }

    /**
     * create a name from a singleNameComponent
     */

    public Name(NameComponent n) throws InvalidName {
        if (n == null)
            throw new InvalidName();
        baseName = n;
        fullName = new NameComponent[1];
        fullName[0] = n;
        ctxName = null;
    }

    /**
     * @return a NameComponent object representing the unstructured base name of this structured name
     */

    public NameComponent baseNameComponent() {
        return baseName;
    }

    public String kind() {
        return baseName.kind;
    }

    /**
     * @return this name as an array of org.omg.CosNaming.NameComponent, neccessary for a number of operations on naming context
     */

    public NameComponent[] components() {
        return fullName;
    }

    /**
     * @return a Name object representing the name of the enclosing context
     */

    public Name ctxName() {
        // null if no further context
        if (ctxName != null) {
            try {
                return new Name(ctxName);
            } catch (InvalidName e) {
                throw new INTERNAL(e.toString());
            }
        }
        return null;
    }

    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof Name))
            return false;
        return (toString().equals(obj.toString()));
    }

    public Name fullName() throws InvalidName {
        return new Name(fullName);
    }

    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * @return the string representation of this name
     */

    public String toString() {
        try {
            return toString(fullName);
        } catch (InvalidName in) {
            return "<invalid>";
        }
    }

    /**
     * @return a single NameComponent, parsed from sn
     */

    private static NameComponent getComponent(String sn)
            throws InvalidName {
        char ch;
        int len = sn.length();
        boolean inKind = false;
        StringBuffer id = new StringBuffer();
        StringBuffer kind = new StringBuffer();

        for (int i = 0; i < len; i++) {
            ch = sn.charAt(i);

            if (ch == '\\') {
                // Escaped character

                i++;
                if (i >= len) {
                    throw new InvalidName();
                }
                ch = sn.charAt(i);
            } else if (ch == '.') {
                // id/kind separator character

                if (inKind) {
                    throw new InvalidName();
                }
                inKind = true;
                continue;
            }
            if (inKind) {
                kind.append(ch);
            } else {
                id.append(ch);
            }
        }

        return (new NameComponent(id.toString(), kind.toString()));
    }

    /**
     *
     * @return an a array of NameComponents
     * @throws InvalidName
     */

    public static NameComponent[] toName(String sn) throws InvalidName {
        if (sn == null || sn.length() == 0 || sn.startsWith("/"))
            throw new InvalidName();

        Vector v = new Vector();

        int start = 0;
        int i = 0;
        for (; i < sn.length(); i++) {
            if (sn.charAt(i) == '/' && sn.charAt(i - 1) != '\\') {
                if (i - start == 0)
                    throw new InvalidName();
                v.addElement(getComponent(sn.substring(start, i)));
                start = i + 1;
            }
        }
        if (start < i)
            v.addElement(getComponent(sn.substring(start, i)));

        NameComponent[] result = new NameComponent[v.size()];

        for (int j = 0; j < result.length; j++) {
            result[j] = (NameComponent) v.elementAt(j);
        }
        return result;
    }

    /**
     * @return the string representation of this NameComponent array
     */

    public static String toString(NameComponent[] n)
            throws InvalidName {
        if (n == null || n.length == 0)
            throw new InvalidName();

        StringBuffer b = new StringBuffer();
        for (int i = 0; i < n.length; i++) {
            if (i > 0)
                b.append("/");

            if (n[i].id.length() > 0)
                b.append(escape(n[i].id));

            if (n[i].kind.length() > 0 || n[i].id.length() == 0)
                b.append(".");

            if (n[i].kind.length() > 0)
                b.append(escape(n[i].kind));
        }
        return b.toString();
    }

    /**
     * escape any occurrence of "/", "." and "\"
     */

    private static String escape(String s) {
        StringBuffer sb = new StringBuffer(s);
        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == '/' || sb.charAt(i) == '\\' || sb.charAt(i) == '.') {
                sb.insert(i, '\\');
                i++;
            }
        }
        return sb.toString();
    }

}

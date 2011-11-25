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
package org.jboss.as.test.integration.ejb.entity.cmp.commerce;


public class FormalName implements java.io.Serializable {
    private String first;
    private char mi;
    private String last;

    public FormalName() {
    }

    public FormalName(String first, char mi, String last) {
        setFirst(first);
        setMi(mi);
        setLast(last);
    }

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        if (first == null) {
            throw new IllegalArgumentException("First is null");
        }
        first = first.trim();
        if (first.length() == 0) {
            throw new IllegalArgumentException("First is zero length");
        }
        this.first = first;
    }

    public char getMi() {
        return mi;
    }

    public void setMi(char mi) {
        this.mi = mi;
    }

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        if (last == null) {
            throw new IllegalArgumentException("Last is null");
        }
        last = last.trim();
        if (last.length() == 0) {
            throw new IllegalArgumentException("Last is zero length");
        }
        this.last = last;
    }

    public boolean equals(Object obj) {
        if (obj instanceof FormalName) {
            FormalName name = (FormalName) obj;
            return equal(name.first, first) &&
                    name.mi == mi &&
                    equal(name.last, last);
        }
        return false;
    }

    private boolean equal(String a, String b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        if (first != null) {
            buf.append(first);
        }
        if (mi != '\u0000') {
            if (first != null) {
                buf.append(" ");
            }
            buf.append(mi).append(".");
        }
        if (last != null) {
            if (first != null || mi != '\u0000') {
                buf.append(" ");
            }
            buf.append(last);
        }
        return buf.toString();
    }
}

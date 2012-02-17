/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.cmp.ejbql;

import java.util.LinkedList;

/**
 * A buffer similar to StringBuffer that works on string blocks instead
 * of individual characters.  This eliminates excessive array allocation
 * and copying at the expense of removal and substring operations. This
 * is a great compromise as usually the only functions called on a
 * StringBuffer are append, length, and toString.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public final class BlockStringBuffer {
    private LinkedList list = new LinkedList();
    private int length;

    public BlockStringBuffer() {
    }

    public BlockStringBuffer append(boolean b) {
        String string = String.valueOf(b);
        length += string.length();
        list.addLast(string);
        return this;
    }

    public BlockStringBuffer append(char c) {
        String string = String.valueOf(c);
        length += string.length();
        list.addLast(string);
        return this;
    }

    public BlockStringBuffer append(char[] str) {
        String string = String.valueOf(str);
        length += string.length();
        list.addLast(string);
        return this;
    }

    public BlockStringBuffer append(char[] str, int offset, int len) {
        String string = String.valueOf(str, offset, len);
        length += string.length();
        list.addLast(string);
        return this;
    }

    public BlockStringBuffer append(double d) {
        String string = String.valueOf(d);
        length += string.length();
        list.addLast(string);
        return this;
    }

    public BlockStringBuffer append(float f) {
        String string = String.valueOf(f);
        length += string.length();
        list.addLast(string);
        return this;
    }

    public BlockStringBuffer append(int i) {
        String string = String.valueOf(i);
        length += string.length();
        list.addLast(string);
        return this;
    }

    public BlockStringBuffer append(long l) {
        String string = String.valueOf(l);
        length += string.length();
        list.addLast(string);
        return this;
    }

    public BlockStringBuffer append(Object obj) {
        if (obj instanceof String) {
            String string = (String) obj;
            length += string.length();
            list.addLast(string);
        } else if (obj instanceof BlockStringBuffer) {
            BlockStringBuffer buf = (BlockStringBuffer) obj;
            length += buf.length;
            list.addAll(buf.list);
        } else {
            String string = String.valueOf(obj);
            length += string.length();
            list.addLast(string);
        }
        return this;
    }

    public BlockStringBuffer prepend(boolean b) {
        String string = String.valueOf(b);
        length += string.length();
        list.addFirst(string);
        return this;
    }

    public BlockStringBuffer prepend(char c) {
        String string = String.valueOf(c);
        length += string.length();
        list.addFirst(string);
        return this;
    }

    public BlockStringBuffer prepend(char[] str) {
        String string = String.valueOf(str);
        length += string.length();
        list.addFirst(string);
        return this;
    }

    public BlockStringBuffer prepend(char[] str, int offset, int len) {
        String string = String.valueOf(str, offset, len);
        length += string.length();
        list.addFirst(string);
        return this;
    }

    public BlockStringBuffer prepend(double d) {
        String string = String.valueOf(d);
        length += string.length();
        list.addFirst(string);
        return this;
    }

    public BlockStringBuffer prepend(float f) {
        String string = String.valueOf(f);
        length += string.length();
        list.addFirst(string);
        return this;
    }

    public BlockStringBuffer prepend(int i) {
        String string = String.valueOf(i);
        length += string.length();
        list.addFirst(string);
        return this;
    }

    public BlockStringBuffer prepend(long l) {
        String string = String.valueOf(l);
        length += string.length();
        list.addFirst(string);
        return this;
    }

    public BlockStringBuffer prepend(Object obj) {
        if (obj instanceof String) {
            String string = (String) obj;
            length += string.length();
            list.addFirst(string);
        } else if (obj instanceof BlockStringBuffer) {
            BlockStringBuffer buf = (BlockStringBuffer) obj;
            length += buf.length;
            list.addAll(0, buf.list);
        } else {
            String string = String.valueOf(obj);
            length += string.length();
            list.addFirst(string);
        }
        return this;
    }

    public int length() {
        return length;
    }

    public int size() {
        return length;
    }

    public StringBuffer toStringBuffer() {
        // use a string buffer because it will share the final buffer
        // with the string object which avoids an allocate and copy
        StringBuffer buf = new StringBuffer(length);

        for (int i = 0; i < list.size(); i++) {
            buf.append((String) list.get(i));
        }
        return buf;
    }

    public String toString() {
        return toStringBuffer().toString();
    }
}

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
package org.jboss.as.controller.property;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A JBossStringBuilder, providing the same functionality as the java5 StringBuilder, except no Appendable which is java5
 * specific.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 *
 */
public class JBossStringBuilder implements Serializable, CharSequence {
    /** Serialization */
    private static final long serialVersionUID = 1874946609763446794L;

    /** The characters */
    protected char[] chars;

    /** The position */
    protected int pos;

    /**
     * Create a new StringBuilder with no characters and an intial size of 16
     */
    public JBossStringBuilder() {
        this(16);
    }

    /**
     * Create a new StringBuilder with no characters
     *
     * @param capacity the initial capacity
     */
    public JBossStringBuilder(int capacity) {
        chars = new char[capacity];
    }

    /**
     * Create a new StringBuilder from the given string. The initial capacity is the length of the string plus 16
     *
     * @param string the string
     */
    public JBossStringBuilder(String string) {
        this(string.length() + 16);
        append(string);
    }

    /**
     * Create a new StringBuilder from the given character sequence. The initial capacity is the length of the sequence plus 16
     *
     * @param charSequence the character sequence
     */
    public JBossStringBuilder(CharSequence charSequence) {
        this(charSequence.length() + 16);
        append(charSequence);
    }

    /**
     * Create a new StringBuilder from the given character array
     *
     * @param ch the array
     */
    public JBossStringBuilder(char[] ch) {
        this(ch, 0, ch.length);
    }

    /**
     * Create a new StringBuilder from the given character array
     *
     * @param ch the array
     * @param start the start of the array from which to take characters
     * @param length the lengh of the array from which to take characters
     */
    public JBossStringBuilder(char[] ch, int start, int length) {
        this(length + 16);
        append(ch, start, length);
    }

    public JBossStringBuilder append(Object object) {
        return append(String.valueOf(object));
    }

    public JBossStringBuilder append(String string) {
        if (string == null)
            string = "null";

        int length = string.length();
        if (length == 0)
            return this;

        int afterAppend = pos + length;
        if (afterAppend > chars.length)
            expandCapacity(afterAppend);

        string.getChars(0, length, chars, pos);
        pos = afterAppend;
        return this;
    }

    public JBossStringBuilder append(StringBuffer buffer) {
        if (buffer == null)
            return append("null");

        int length = buffer.length();
        if (length == 0)
            return this;

        int afterAppend = pos + length;
        if (afterAppend > chars.length)
            expandCapacity(afterAppend);

        buffer.getChars(0, length, chars, pos);
        pos = afterAppend;
        return this;
    }

    public JBossStringBuilder append(CharSequence charSequence) {
        if (charSequence == null)
            return append("null");

        int length = charSequence.length();
        if (length == 0)
            return this;

        return append(charSequence, 0, charSequence.length());
    }

    public JBossStringBuilder append(CharSequence charSequence, int start, int end) {
        if (charSequence == null)
            return append("null");

        if (start < 0 || end < 0 || start > end || start > charSequence.length())
            throw new IndexOutOfBoundsException("Invalid start=" + start + " end=" + end + " length=" + charSequence.length());

        int length = end - start;
        if (length == 0)
            return this;

        int afterAppend = pos + length;
        if (afterAppend > chars.length)
            expandCapacity(afterAppend);

        for (int i = start; i < end; ++i)
            chars[pos++] = charSequence.charAt(i);
        pos = afterAppend;
        return this;
    }

    public JBossStringBuilder append(char[] array) {
        if (array == null)
            return append("null");

        if (array.length == 0)
            return this;

        String string = String.valueOf(array);
        return append(string);
    }

    public JBossStringBuilder append(char[] array, int offset, int length) {
        if (array == null)
            return append("null");

        int arrayLength = array.length;
        if (offset < 0 || length < 0 || offset + length > arrayLength)
            throw new IndexOutOfBoundsException("Invalid offset=" + offset + " length=" + length + " array.length="
                    + arrayLength);

        if (length == 0 || arrayLength == 0)
            return this;

        String string = String.valueOf(array, offset, length);
        return append(string);
    }

    public JBossStringBuilder append(boolean primitive) {
        String string = String.valueOf(primitive);
        return append(string);
    }

    public JBossStringBuilder append(char primitive) {
        String string = String.valueOf(primitive);
        return append(string);
    }

    public JBossStringBuilder append(int primitive) {
        String string = String.valueOf(primitive);
        return append(string);
    }

    public JBossStringBuilder append(long primitive) {
        String string = String.valueOf(primitive);
        return append(string);
    }

    public JBossStringBuilder append(float primitive) {
        String string = String.valueOf(primitive);
        return append(string);
    }

    public JBossStringBuilder append(double primitive) {
        String string = String.valueOf(primitive);
        return append(string);
    }

    public JBossStringBuilder delete(int start, int end) {
        if (start < 0 || start > pos || start > end || end > pos)
            throw new IndexOutOfBoundsException("Invalid start=" + start + " end=" + end + " length=" + pos);

        if (start == end)
            return this;

        int removed = end - start;
        System.arraycopy(chars, start + removed, chars, start, pos - end);
        pos -= removed;
        return this;
    }

    public JBossStringBuilder deleteCharAt(int index) {
        return delete(index, 1);
    }

    public JBossStringBuilder replace(int start, int end, String string) {
        delete(start, end);
        return insert(start, string);
    }

    public JBossStringBuilder insert(int index, char[] string) {
        return insert(index, string, 0, string.length);
    }

    public JBossStringBuilder insert(int index, char[] string, int offset, int len) {
        int stringLength = string.length;
        if (index < 0 || index > pos || offset < 0 || len < 0 || (offset + len) > string.length)
            throw new IndexOutOfBoundsException("Invalid index=" + index + " offset=" + offset + " len=" + len
                    + " string.length=" + stringLength + " length=" + pos);

        if (len == 0)
            return this;

        int afterAppend = pos + len;
        if (afterAppend > chars.length)
            expandCapacity(afterAppend);

        System.arraycopy(chars, index, chars, index + stringLength, pos - index);
        System.arraycopy(string, offset, chars, index, len);
        pos = afterAppend;
        return this;
    }

    public JBossStringBuilder insert(int offset, Object object) {
        if (object == null)
            return insert(offset, "null");
        else
            return insert(offset, String.valueOf(object));
    }

    public JBossStringBuilder insert(int offset, String string) {
        if (offset < 0 || offset > pos)
            throw new IndexOutOfBoundsException("Invalid offset=" + offset + " length=" + pos);

        if (string == null)
            string = "null";

        int stringLength = string.length();

        int afterAppend = pos + stringLength;
        if (afterAppend > chars.length)
            expandCapacity(afterAppend);

        System.arraycopy(chars, offset, chars, offset + stringLength, pos - offset);
        string.getChars(0, stringLength, chars, offset);
        pos = afterAppend;
        return this;
    }

    public JBossStringBuilder insert(int offset, CharSequence charSequence) {
        if (charSequence == null)
            return insert(offset, "null");
        else
            return insert(offset, charSequence, 0, charSequence.length());
    }

    public JBossStringBuilder insert(int offset, CharSequence charSequence, int start, int end) {
        if (charSequence == null)
            charSequence = "null";

        int sequenceLength = charSequence.length();
        if (offset < 0 || offset > pos || start < 0 || end < 0 || start > sequenceLength || end > sequenceLength || start > end)
            throw new IndexOutOfBoundsException("Invalid offset=" + offset + " start=" + start + " end=" + end
                    + " sequence.length()=" + sequenceLength + " length=" + pos);

        int len = end - start;
        if (len == 0)
            return this;

        int afterAppend = pos + len;
        if (afterAppend > chars.length)
            expandCapacity(afterAppend);

        System.arraycopy(chars, offset, chars, offset + sequenceLength, pos - offset);
        for (int i = start; i < end; ++i)
            chars[offset++] = charSequence.charAt(i);
        pos = afterAppend;
        return this;
    }

    public JBossStringBuilder insert(int offset, boolean primitive) {
        return insert(offset, String.valueOf(primitive));
    }

    public JBossStringBuilder insert(int offset, char primitive) {
        return insert(offset, String.valueOf(primitive));
    }

    public JBossStringBuilder insert(int offset, int primitive) {
        return insert(offset, String.valueOf(primitive));
    }

    public JBossStringBuilder insert(int offset, long primitive) {
        return insert(offset, String.valueOf(primitive));
    }

    public JBossStringBuilder insert(int offset, float primitive) {
        return insert(offset, String.valueOf(primitive));
    }

    public JBossStringBuilder insert(int offset, double primitive) {
        return insert(offset, String.valueOf(primitive));
    }

    public int indexOf(String string) {
        return indexOf(string, 0);
    }

    public int indexOf(String string, int fromIndex) {
        // FIXME
        return toString().indexOf(string, fromIndex);
    }

    public int indexOf(char ch) {
        return indexOf(ch, 0);
    }

    public int indexOf(char ch, int fromIndex) {
        // FIXME
        return toString().indexOf(ch, fromIndex);
    }

    public int lastIndexOf(String string) {
        return lastIndexOf(string, pos);
    }

    public int lastIndexOf(String string, int fromIndex) {
        // FIXME
        return toString().lastIndexOf(string, fromIndex);
    }

    public int lastIndexOf(char ch) {
        return lastIndexOf(ch, pos);
    }

    public int lastIndexOf(char ch, int fromIndex) {
        // FIXME
        return toString().lastIndexOf(ch, fromIndex);
    }

    public JBossStringBuilder reverse() {
        char[] tmp = new char[pos];
        for (int n = 0; n < pos; n++)
            tmp[n] = chars[pos - n - 1];
        chars = tmp;
        return this;
    }

    public String toString() {
        return new String(chars, 0, pos);
    }

    public int length() {
        return pos;
    }

    public int capacity() {
        return chars.length;
    }

    public void ensureCapacity(int minimum) {
        if (minimum < 0 || minimum < chars.length)
            return;
        expandCapacity(minimum);
    }

    public void trimToSize() {
        char[] trimmed = new char[pos];
        System.arraycopy(chars, 0, trimmed, 0, pos);
        chars = trimmed;
    }

    public void setLength(int newLength) {
        if (newLength < 0)
            throw new StringIndexOutOfBoundsException(newLength);
        if (newLength > chars.length)
            expandCapacity(newLength);
        Arrays.fill(chars, newLength, chars.length, '\0');
        pos = newLength;
    }

    public char charAt(int index) {
        return chars[index];
    }

    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        if (srcBegin < 0 || dstBegin < 0 || srcBegin > srcEnd || srcEnd > pos || (dstBegin + srcEnd - srcBegin) > dst.length)
            throw new IndexOutOfBoundsException("Invalid srcBegin=" + srcBegin + " srcEnd=" + srcEnd + " dstBegin=" + dstBegin
                    + " dst.length=" + dst.length + " length=" + pos);

        int len = srcEnd - srcBegin;
        if (len == 0)
            return;

        System.arraycopy(chars, srcBegin, dst, dstBegin, len);
    }

    public void setCharAt(int index, char ch) {
        if (index < 0 || index > pos)
            throw new IndexOutOfBoundsException("Invalid index=" + index + " length=" + pos);

        chars[index] = ch;
    }

    public String substring(int start) {
        return substring(start, pos);
    }

    public CharSequence subSequence(int start, int end) {
        return substring(start, end);
    }

    public String substring(int start, int end) {
        if (start < 0 || end < 0 || start > end || end > pos)
            throw new IndexOutOfBoundsException("Invalid start=" + start + " end=" + end + " length=" + pos);

        return new String(chars, start, end - start);
    }

    /**
     * Expand the capacity to the greater of the minimum or twice the current size
     *
     * @param minimum the minimum
     */
    protected void expandCapacity(int minimum) {
        int newSize = chars.length * 2;
        if (minimum > newSize)
            newSize = minimum;

        char[] newChars = new char[newSize];
        System.arraycopy(chars, 0, newChars, 0, pos);
        chars = newChars;
    }
}

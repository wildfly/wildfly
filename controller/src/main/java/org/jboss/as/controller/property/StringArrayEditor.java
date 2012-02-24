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

import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * A property editor for String[]. The text format of a string array is a comma or \n, \r seperated list with \, representing an
 * escaped comma to include in the string element.
 *
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author Scott.Stark@jboss.org
 */
public class StringArrayEditor extends PropertyEditorSupport {
    Pattern commaDelim = Pattern.compile("','|[^,\r\n]+");

    static String[] parseList(String text) {
        ArrayList<String> list = new ArrayList<String>();
        StringBuffer tmp = new StringBuffer();
        for (int n = 0; n < text.length(); n++) {
            char c = text.charAt(n);
            switch (c) {
                case '\\':
                    tmp.append(c);
                    if (n < text.length() && text.charAt(n + 1) == ',') {
                        tmp.setCharAt(tmp.length() - 1, ',');
                        n++;
                    }
                    break;
                case ',':
                case '\n':
                case '\r':
                    if (tmp.length() > 0)
                        list.add(tmp.toString());
                    tmp.setLength(0);
                    break;
                default:
                    tmp.append(c);
                    break;
            }
        }
        if (tmp.length() > 0)
            list.add(tmp.toString());

        String[] x = new String[list.size()];
        list.toArray(x);
        return x;
    }

    /**
     * Build a String[] from comma or eol seperated elements with a \, representing a ',' to include in the current string
     * element.
     *
     */
    public void setAsText(final String text) {
        String[] theValue = parseList(text);
        setValue(theValue);
    }

    /**
     * @return a comma seperated string of the array elements
     */
    public String getAsText() {
        String[] theValue = (String[]) getValue();
        StringBuffer text = new StringBuffer();
        int length = theValue == null ? 0 : theValue.length;
        for (int n = 0; n < length; n++) {
            String s = theValue[n];
            if (s.equals(","))
                text.append('\\');
            text.append(s);
            text.append(',');
        }
        // Remove the trailing ','
        if (text.length() > 0)
            text.setLength(text.length() - 1);
        return text.toString();
    }
}

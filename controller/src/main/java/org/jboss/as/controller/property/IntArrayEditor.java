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
import java.util.StringTokenizer;

/**
 * A property editor for int[].
 *
 *
 */
public class IntArrayEditor extends PropertyEditorSupport {
    /**
     * Build a int[] from comma or eol seperated elements
     *
     */
    public void setAsText(final String text) {
        StringTokenizer stok = new StringTokenizer(text, ",\r\n");
        int[] theValue = new int[stok.countTokens()];
        int i = 0;
        while (stok.hasMoreTokens()) {
            theValue[i++] = Integer.decode(stok.nextToken()).intValue();
        }
        setValue(theValue);
    }

    /**
     * @return a comma seperated string of the array elements
     */
    public String getAsText() {
        int[] theValue = (int[]) getValue();
        StringBuffer text = new StringBuffer();
        int length = theValue == null ? 0 : theValue.length;
        for (int n = 0; n < length; n++) {
            if (n > 0)
                text.append(',');
            text.append(theValue[n]);
        }
        return text.toString();
    }
}

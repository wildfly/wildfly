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
package org.jboss.as.jdr.util;

import org.jboss.vfs.VirtualFileFilter;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link Sanitizer} subclass that replaces all instance of {@code pattern} with
 * the {@code replacement} text.
 */
public class PatternSanitizer extends AbstractSanitizer {

    private final Pattern pattern;
    private final String replacement;

    public PatternSanitizer(String pattern, String replacement, VirtualFileFilter filter) throws Exception {
        this.pattern = Pattern.compile(pattern);
        this.replacement = replacement;
        this.filter = filter;
    }

    public InputStream sanitize(InputStream in) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(output);
        String[] lines = Utils.readLines(in).toArray(new String[0]);
        int lineCount = lines.length;
        for(int i = 0; i < lineCount; i++) {
            Matcher matcher = pattern.matcher(lines[i]);
            writer.write(matcher.replaceAll(replacement));
            if(i < (lineCount-1)){
                writer.write(Utils.LINE_SEP);
            }
        }
        writer.close();
        return new ByteArrayInputStream(output.toByteArray());
    }
}

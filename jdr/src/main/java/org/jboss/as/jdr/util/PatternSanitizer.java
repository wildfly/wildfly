/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

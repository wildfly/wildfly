package org.jboss.as.jdr.util;

import org.jboss.as.jdr.vfs.Filters;

public class Sanitizers {

    public static Sanitizer pattern(String pattern, String replacement) throws Exception {
        return new PatternSanitizer(pattern, replacement, Filters.suffix(".properties"));
    }

    public static Sanitizer xml(String xpath) throws Exception {
        return new XMLSanitizer(xpath, Filters.suffix(".xml"));
    }
}

package org.jboss.as.jdr.util;

import org.jboss.as.jdr.vfs.Filters;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;

import java.io.InputStream;

abstract class AbstractSanitizer implements Sanitizer {

    protected VirtualFileFilter filter = Filters.TRUE;

    @Override
    public abstract InputStream sanitize(InputStream in) throws Exception;


    @Override
    public boolean accepts(VirtualFile resource) {
        return filter.accepts(resource);
    }
}

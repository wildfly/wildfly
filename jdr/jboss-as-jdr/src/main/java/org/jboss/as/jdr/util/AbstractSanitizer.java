package org.jboss.as.jdr.util;

import org.jboss.as.jdr.vfs.Filters;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;

import java.io.InputStream;

/**
 * Provides a default implementation of {@link Sanitizer} that uses default
 * filtering. Sanitizers should subclass this unless they wish to use complex
 * accepts filtering.
 */
abstract class AbstractSanitizer implements Sanitizer {

    protected VirtualFileFilter filter = Filters.TRUE;

    @Override
    public abstract InputStream sanitize(InputStream in) throws Exception;


    /**
     * returns whether or not a VirtualFile should be processed by this sanitizer.
     *
     * @param resource {@link VirtualFile} resource to test
     * @return
     */
    @Override
    public boolean accepts(VirtualFile resource) {
        return filter.accepts(resource);
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jdr.util;

import org.jboss.vfs.VirtualFile;

import java.io.InputStream;

public interface Sanitizer {
    InputStream sanitize(InputStream in) throws Exception;
    boolean accepts(VirtualFile resource);
}

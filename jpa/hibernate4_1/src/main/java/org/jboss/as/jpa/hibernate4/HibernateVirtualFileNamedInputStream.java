/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.jpa.hibernate4;

import static org.jboss.as.jpa.hibernate4.JpaLogger.JPA_LOGGER;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.vfs.VirtualFile;

/**
 * VFS named input stream.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author Scott Marlow
 */
public class HibernateVirtualFileNamedInputStream extends HibernateLazyNamedInputStream {
    private VirtualFile file;

    private static String name(VirtualFile file) {
        if (file == null)
            throw JPA_LOGGER.nullVar("file");
        return file.getName();
    }

    public HibernateVirtualFileNamedInputStream(VirtualFile file) {
        super(name(file));
        this.file = file;
    }

    protected InputStream getLazyStream() throws IOException {
        return file.openStream();
    }
}
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
package org.jboss.as.jpa.hibernate5;

import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.jboss.vfs.VFS;


/**
 * In Hibernate terms, the ArchiveDescriptorFactory contract is used to plug in handling for how to deal
 * with archives in various systems.  For JBoss, that means its VirtualFileSystem API.
 *
 * @author Steve Ebersole
 */
public class VirtualFileSystemArchiveDescriptorFactory extends StandardArchiveDescriptorFactory {
    public static final VirtualFileSystemArchiveDescriptorFactory INSTANCE = new VirtualFileSystemArchiveDescriptorFactory();

    @Override
    public ArchiveDescriptor buildArchiveDescriptor(URL url, String entryBase) {
        try {
            return new VirtualFileSystemArchiveDescriptor( VFS.getChild( url.toURI() ), entryBase );
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException( e );
        }
    }
}

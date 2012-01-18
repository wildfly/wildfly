/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.server.moduleservice;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.modules.Module;

/**
 * Utility class for read a composite index from a system module.
 *
 * @author Stuart Douglas
 */
public class ModuleIndexBuilder {

    public static final String INDEX_LOCATION = "META-INF/jandex.idx";

    public static CompositeIndex buildCompositeIndex(Module module) {
        try {
            final Enumeration<URL> resources = module.getClassLoader().getResources(INDEX_LOCATION);
            if (!resources.hasMoreElements()) {
                return null;
            }
            final Set<Index> indexes = new HashSet<Index>();
            while (resources.hasMoreElements()) {
                final URL url = resources.nextElement();
                InputStream stream = url.openStream();
                try {
                    IndexReader reader = new IndexReader(stream);
                    indexes.add(reader.read());
                } finally {
                    stream.close();
                }
            }
            return new CompositeIndex(indexes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private ModuleIndexBuilder() {

    }

}

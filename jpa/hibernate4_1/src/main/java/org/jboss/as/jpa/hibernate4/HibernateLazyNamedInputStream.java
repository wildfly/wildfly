/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.hibernate4;

import static org.jipijapa.JipiLogger.JPA_LOGGER;

import java.io.IOException;
import java.io.InputStream;

import org.hibernate.ejb.packaging.NamedInputStream;

/**
 * Lazy named input stream.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 *         Scott Marlow
 */
public abstract class HibernateLazyNamedInputStream extends NamedInputStream {
    public HibernateLazyNamedInputStream(String name) {
        super(name, null);
    }

    /**
     * Get lazy input stream.
     *
     * @return the input stream
     * @throws java.io.IOException for any I/O error
     */
    protected abstract InputStream getLazyStream() throws IOException;

    @Override
    public InputStream getStream() {
        try {
            return getLazyStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setStream(InputStream stream) {
        throw JPA_LOGGER.cannotChangeInputStream();
    }
}

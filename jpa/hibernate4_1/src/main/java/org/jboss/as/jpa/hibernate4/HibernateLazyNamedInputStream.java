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

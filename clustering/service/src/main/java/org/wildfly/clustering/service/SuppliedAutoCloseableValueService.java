/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.service;

import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

/**
 * Generic {@link org.jboss.msc.Service} whose provided closeable value is a mapping of a supplied value.
 * @author Paul Ferraro
 */
public class SuppliedAutoCloseableValueService<T extends AutoCloseable, V> extends SuppliedValueService<T, V> {

    private static final Logger LOGGER = Logger.getLogger(SuppliedAutoCloseableValueService.class);

    /**
     * Constructs a new supplied closeable value service.
     * @param mapper a function that maps the supplied value to the service value
     * @param supplier produces the supplied closeable value
     */
    public SuppliedAutoCloseableValueService(Function<T, V> mapper, Supplier<T> supplier) {
        super(mapper, supplier, value -> {
            try {
                value.close();
            } catch (Exception e) {
                LOGGER.warn(e.getLocalizedMessage(), e);
            }
        });
    }
}

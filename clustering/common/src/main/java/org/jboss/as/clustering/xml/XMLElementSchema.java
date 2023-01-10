/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.xml;

import java.util.EnumSet;

import org.jboss.staxmapper.XMLMapper;

/**
 * A schema that exposes its own {@link XMLElementReader}.
 * @author Paul Ferraro
 */
public interface XMLElementSchema<T, N extends Schema<N>> extends Schema<N>, XMLElementReaderFactory<T, N> {

    /**
     * Creates a StAX mapper from an enumeration of schemas.
     * @param <C> the xml reader context type
     * @param <S> the schema type
     * @param schemaClass a schema enum class
     * @return a StAX mapper
     */
    static <C, S extends Enum<S> & XMLElementSchema<C, S>> XMLMapper createMapper(Class<S> schemaClass) {
        XMLMapper mapper = XMLMapper.Factory.create();
        for (S schema : EnumSet.allOf(schemaClass)) {
            // Register via supplier so that reader is created on-demand, and garbage collected when complete
            mapper.registerRootElement(schema.getName(), schema);
        }
        return mapper;
    }
}

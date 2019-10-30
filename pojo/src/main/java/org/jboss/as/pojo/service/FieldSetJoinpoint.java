/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.pojo.service;

import org.jboss.as.pojo.logging.PojoLogger;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * Field set joinpoint.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class FieldSetJoinpoint extends FieldJoinpoint {
    public FieldSetJoinpoint(Field field) {
        super(field);
    }

    @Override
    public Object dispatch() throws Throwable {
        Object[] params = toObjects(new Type[]{getField().getGenericType()});
        if (params == null || params.length != 1)
            throw PojoLogger.ROOT_LOGGER.illegalParameterLength(Arrays.toString(params));

        getField().set(getTarget().getValue(), params[0]);
        return null;
    }
}

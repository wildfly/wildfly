/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

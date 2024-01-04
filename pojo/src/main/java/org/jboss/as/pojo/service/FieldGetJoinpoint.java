/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.service;

import java.lang.reflect.Field;

/**
 * Field get joinpoint.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class FieldGetJoinpoint extends FieldJoinpoint {
    public FieldGetJoinpoint(Field field) {
        super(field);
    }

    @Override
    public Object dispatch() throws Throwable {
        return getField().get(getTarget().getValue());
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.service;

import java.lang.reflect.Field;

/**
 * Field joinpoint.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class FieldJoinpoint extends TargetJoinpoint {
    private final Field field;

    public FieldJoinpoint(Field field) {
        this.field = field;
    }

    protected Field getField() {
        return field;
    }
}

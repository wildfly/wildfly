/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.service;

import java.lang.reflect.Constructor;

/**
 * Ctor joinpoint.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ConstructorJoinpoint extends AbstractJoinpoint {
    private final Constructor ctor;

    public ConstructorJoinpoint(Constructor ctor) {
        this.ctor = ctor;
    }

    @Override
    public Object dispatch() throws Throwable {
        return ctor.newInstance(toObjects(ctor.getGenericParameterTypes()));
    }
}

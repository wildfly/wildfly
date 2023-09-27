/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.service;

import org.jboss.msc.value.Value;

/**
 * Target joinpoint; keeps target.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class TargetJoinpoint extends AbstractJoinpoint {
    private Value<Object> target;

    public Value<Object> getTarget() {
        return target;
    }

    public void setTarget(Value<Object> target) {
        this.target = target;
    }
}

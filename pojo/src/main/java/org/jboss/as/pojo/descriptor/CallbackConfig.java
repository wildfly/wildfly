/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.descriptor;

import org.jboss.as.pojo.BeanState;

import java.io.Serializable;

/**
 * Callback meta data.
 * Atm this is simplified version of what we had in JBossAS5/6.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CallbackConfig extends AbstractConfigVisitorNode implements Serializable {
    private static final long serialVersionUID = 1L;

    private String methodName;
    private BeanState whenRequired = BeanState.INSTALLED;
    private BeanState state = BeanState.INSTALLED;
    private String signature;

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public BeanState getWhenRequired() {
        return whenRequired;
    }

    public void setWhenRequired(BeanState whenRequired) {
        this.whenRequired = whenRequired;
    }

    public BeanState getState() {
        return state;
    }

    public void setState(BeanState state) {
        this.state = state;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
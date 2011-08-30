/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
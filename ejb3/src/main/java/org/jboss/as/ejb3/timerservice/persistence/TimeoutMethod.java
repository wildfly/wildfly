/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.timerservice.persistence;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * TimeoutMethod
 *
 * @author Jaikiran Pai
 * @author Stuart Douglas
 */
public class TimeoutMethod implements Serializable {
    private static final long serialVersionUID = 3306711742026221772L;

    /**
     * Constant string value to indicate that the timeout method has 1 parameter, which must be javax.ejb.Timer
     */
    public static final String TIMER_PARAM_1 = "1";

    /**
     * Constant string array value to indicate that the timeout method has 1
     * parameter, which must be javax.ejb.Timer
     */
    public static final String[] TIMER_PARAM_1_ARRAY = new String[]{TIMER_PARAM_1};

    /**
     * Internal immutable string list to indicate that the timeout method has 1
     * parameter, which must be javax.ejb.Timer
     */
    private static final List<String> TIMER_PARAM_1_LIST = Collections.singletonList("javax.ejb.Timer");

    private String declaringClass;

    private String methodName;

    private List<String> methodParams;

    public TimeoutMethod(String declaringClass, String methodName, String[] methodParams) {
        this.declaringClass = declaringClass;
        this.methodName = methodName;
        if (methodParams == null || methodParams.length == 0) {
            this.methodParams = Collections.emptyList();
        } else {
            this.methodParams = TIMER_PARAM_1_LIST;
        }
    }

    public String getMethodName() {
        return methodName;
    }

    public boolean hasTimerParameter() {
        return methodParams == TIMER_PARAM_1_LIST;
    }

    public String getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.declaringClass);
        sb.append(".");
        sb.append(this.methodName);
        sb.append("(");
        if (!this.methodParams.isEmpty()) {
            sb.append(this.methodParams.get(0));
        }
        sb.append(")");
        return sb.toString();
    }
}

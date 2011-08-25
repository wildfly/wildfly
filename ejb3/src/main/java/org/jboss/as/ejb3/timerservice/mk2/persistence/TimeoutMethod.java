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
package org.jboss.as.ejb3.timerservice.mk2.persistence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TimeoutMethod
 *
 * @author Jaikiran Pai
 * @author Stuart Douglas
 */
public class TimeoutMethod implements Serializable {

    private Long id;

    private String declaringClass;

    private String methodName;

    private List<String> methodParams;

    private transient String cachedToString;

    public TimeoutMethod() {

    }

    public TimeoutMethod(String declaringClass, String methodName, String[] methodParams) {
        this.declaringClass = declaringClass;
        this.methodName = methodName;
        if (methodParams != null) {
            this.methodParams = new ArrayList<String>(Arrays.asList(methodParams));
        }
    }

    public Long getId() {
        return id;
    }

    public String getMethodName() {
        return methodName;
    }


    public String[] getMethodParams() {
        if (this.methodParams == null) {
            return null;
        }
        return methodParams.toArray(new String[]{});
    }

    public String getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public String toString() {
        if (this.cachedToString == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.declaringClass);
            sb.append(".");
            sb.append(this.methodName);
            sb.append("(");
            if (this.methodParams != null) {
                for (int i = 0; i < this.methodParams.size(); i++) {
                    sb.append(this.methodParams.get(i));
                    if (i != this.methodParams.size() - 1) {
                        sb.append(",");
                    }
                }
            }
            sb.append(")");
            this.cachedToString = sb.toString();
        }
        return this.cachedToString;
    }

}

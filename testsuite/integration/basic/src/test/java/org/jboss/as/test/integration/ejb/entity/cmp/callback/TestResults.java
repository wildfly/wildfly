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

package org.jboss.as.test.integration.ejb.entity.cmp.callback;

import java.util.HashMap;
import java.util.Map;

/**
 * A helper class to check the method invocations with arquillian tests.
 *
 * @author <a href="mailto:wfink@redhat.com">Wolf-Dieter Fink</a>
 *
 */
public class TestResults {
    private TestResults(){}

    private static Map<String, Integer> methodMap = new HashMap<String, Integer>();

    public static int getNumberOfCalls(String methodIdentifier) {
        return methodMap.containsKey(methodIdentifier) ? methodMap.get(methodIdentifier) : 0;
    }
    public static boolean isCalled(String methodIdentifier) {
        return methodMap.containsKey(methodIdentifier) && methodMap.get(methodIdentifier) > 0;
    }
    public static void setMethodExecuted(String methodIdentifier) {
        if(methodMap.containsKey(methodIdentifier)) {
            methodMap.put(methodIdentifier, methodMap.get(methodIdentifier)+1);
        }else{
            methodMap.put(methodIdentifier, 1);
        }
    }

    public static void resetMethod(String methodIdentifier) {
        methodMap.remove(methodIdentifier);
    }
    public static void resetAll() {
        methodMap.clear();
    }
}

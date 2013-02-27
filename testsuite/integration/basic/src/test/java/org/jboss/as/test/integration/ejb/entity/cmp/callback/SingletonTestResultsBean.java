/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * Singleton to register the callback invocations to be able to check it from the UnitTest.
 *
 * @author <a href="mailto:wfink@redhat.com">Wolf-Dieter Fink</a>
 */
@Singleton
@Startup
public class SingletonTestResultsBean implements SingletonTestResults {
    private static Map<String, Integer> methodMap = new HashMap<String, Integer>();

    @Override
    public int getNumberOfCalls(String clazz, String pKey, String callbackName) {
        final String methodIdentifier = clazz + "#" + pKey + "." + callbackName;
        return methodMap.containsKey(methodIdentifier) ? methodMap.get(methodIdentifier) : 0;
    }
    @Override
    public boolean isCalled(String clazz, String pKey, String callbackName) {
        final String methodIdentifier = clazz + "#" + pKey + "." + callbackName;
        return methodMap.containsKey(methodIdentifier) && methodMap.get(methodIdentifier) > 0;
    }
    @Override
    public void setMethodExecuted(String clazz, String pKey, String callbackName) {
        final String methodIdentifier = clazz + "#" + pKey + "." + callbackName;
        if(methodMap.containsKey(methodIdentifier)) {
            methodMap.put(methodIdentifier, methodMap.get(methodIdentifier)+1);
        }else{
            methodMap.put(methodIdentifier, 1);
        }
    }

    @Override
    public void resetAll() {
        methodMap.clear();
    }

    @Override
    public String showAll() {
        return methodMap.toString();
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.classloading.ear.subdeployments;

public class ClassInJar {
    public String invokeToStringOnClassloaderOfClass (String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
       ClassLoader cl = Class.forName(className).getClassLoader();
       if(cl == null) {
           return "bootstrap class loader";
       }
       return cl.toString();
    }
}

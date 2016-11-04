package org.jboss.as.test.integration.deployment.classloading.ear.subdeployments;

public class ClassInJar {
    public String invokeToStringOnInstance (String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
       // Invoke toString() method on className
       return Class.forName(className).newInstance().toString();
    }
}

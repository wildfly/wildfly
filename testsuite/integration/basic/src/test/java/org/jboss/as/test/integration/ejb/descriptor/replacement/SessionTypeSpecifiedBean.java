/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.integration.ejb.descriptor.replacement;

/**
 *
 * @author rhatlapa
 */
public class SessionTypeSpecifiedBean {
    
    private String name;
    
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * @param name used by greeting to make name more personal and pointing to person (or some other) name
     * @return greeting in format "Hi $name" where $name is taken from method parameter
     */
    public String greet(String name) {
        return "Hi " + name;
    }
    
    /**
     * @return greeting in format "Hi $name" where $name is taken from object attribute
     */
    public String greet() {
        return "Hi " + name;
    }
}

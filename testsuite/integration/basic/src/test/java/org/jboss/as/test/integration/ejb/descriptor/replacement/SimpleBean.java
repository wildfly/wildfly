/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.integration.ejb.descriptor.replacement;

/**
 * Bean for testing descriptor element <after-begin-method> 
 * @author rhatlapa
 */
public class SimpleBean {    
   
    private String name;
    
    public String sayHello() {
        return "Hello " + name;
    }
    
    public void setNameToAnonym() {
        this.name = "Anonym";
    }
    
    public void setNameToSomebody() {
        this.name = "Somebody";
    }
    
    public void setNameToUnknown() {
        this.name = "Unknown";
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }
}

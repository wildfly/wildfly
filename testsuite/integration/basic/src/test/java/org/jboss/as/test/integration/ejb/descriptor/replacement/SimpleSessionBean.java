/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.integration.ejb.descriptor.replacement;

/**
 *
 * @author rhatlapa
 */
public class SimpleSessionBean implements SessionBean {

    public String greet() {
        return "Greetings";
    }
    
}

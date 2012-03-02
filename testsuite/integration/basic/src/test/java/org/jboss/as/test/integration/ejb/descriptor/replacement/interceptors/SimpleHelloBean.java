/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.integration.ejb.descriptor.replacement.interceptors;

/**
 *
 * @author rhatlapa
 */
public class SimpleHelloBean {
    
    public String hello(String name) {
        return "Hello " + name;
    }
}

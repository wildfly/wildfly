package org.jboss.as.test.integration.weld.multideployment;

import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
public class ModuleEjb {

    public String sayHello() {
        return "hello";
    }
}

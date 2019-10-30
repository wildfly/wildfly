package org.jboss.as.test.integration.weld.multideployment;

import javax.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
public class ModuleEjb {

    public String sayHello() {
        return "hello";
    }
}

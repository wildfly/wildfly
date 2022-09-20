package org.jboss.as.test.integration.ejb.injection.injectiontarget;

import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
public class InjectingBean {

    private SuperInterface injected;

    public String getName() {
        return injected.getName();
    }

}

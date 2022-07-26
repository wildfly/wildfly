package org.jboss.as.test.integration.ejb.injection.injectiontarget;

import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
public class InjectedBean implements SubInterface{
    @Override
    public String getName() {
        return InjectedBean.class.getName();
    }
}

package org.jboss.as.test.integration.weld.implicitcomponents;

import javax.annotation.ManagedBean;
import javax.inject.Inject;

/**
 * @author Stuart Douglas
 */
@ManagedBean
public class ManagedBean1 {

    @Inject
    private ManagedBean2 bean;

    public String getMessage() {
        return bean.getMessage();
    }

}

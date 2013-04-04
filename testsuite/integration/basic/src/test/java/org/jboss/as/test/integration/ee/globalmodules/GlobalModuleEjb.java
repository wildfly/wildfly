package org.jboss.as.test.integration.ee.globalmodules;

import javax.ejb.Stateless;
import javax.interceptor.Interceptors;

/**
 * @author Stuart Douglas
 */
@Stateless
@Interceptors(GlobalModuleInterceptor.class)
public class GlobalModuleEjb {

    public String getName() {
        return GlobalModuleEjb.class.getSimpleName();
    }


}


package org.jboss.as.test.integration.ee.globalmodules;

import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

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


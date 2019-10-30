package org.jboss.as.test.integration.weld.interceptor.bridgemethods;

import javax.ejb.Local;
import javax.ejb.Stateless;

/**
 *
 */
@Stateless
@Local(SpecialService.class)
@SomeInterceptorBinding
public class SpecialServiceImpl implements SpecialService {

    public void doSomething(String param) {
        //System.out.println("SpecialServiceImpl.doSomething");
    }
}
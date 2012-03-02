/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.integration.ejb.descriptor.replacement.interceptors;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import junit.framework.Assert;

/**
 *
 * @author rhatlapa
 */
public class InterceptorTests {

    /**
     * tests interceptor by calling bean with method which has behavior changed by interceptor
     * @param iniCtx
     * @param expected is value expected to be returned by intercepted bean
     * @throws NamingException 
     */
    protected void testInterceptor(InitialContext iniCtx, String expected) throws NamingException {
        final SimpleHelloBean helloBean = (SimpleHelloBean) iniCtx.lookup("java:module/SimpleHelloBean");
        Assert.assertEquals("Interception method wasn't changed by jboss spec descriptor", expected, helloBean.
                hello(""));
    }
}

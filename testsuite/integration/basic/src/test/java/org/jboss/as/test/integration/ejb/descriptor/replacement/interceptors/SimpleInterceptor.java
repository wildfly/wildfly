/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.integration.ejb.descriptor.replacement.interceptors;

import javax.interceptor.InvocationContext;

/**
 *
 * @author rhatlapa
 */
public class SimpleInterceptor {

    /**
     * interceptor defined in ejb-spec descriptor ejb-jar.xml which appends at start of the first 
     * parameter of the intercepted method string distinguishing that this interceptor has been called 
     * (string used: EjbIntercepted)
     * @param ctx
     * @return
     * @throws Exception 
     */
    public Object helloIntercept(InvocationContext ctx)
            throws Exception {
        Object[] params = ctx.getParameters();
        String name = (String) params[0];
        params[0] = "EjbIntercepted" + name;
        ctx.setParameters(params);
        Object res = ctx.proceed();
        return res;
    }
    
    /**
     * interceptor defined in jboss-spec descriptor jboss-ejb3.xml which appends at start of the first 
     * parameter of the intercepted method string showing that this interceptor method has been called
     * (string used: JbossSpecIntercepted)
     * @param ctx
     * @return
     * @throws Exception 
     */
    public Object redefinedHelloIntercept(InvocationContext ctx)
            throws Exception {
        Object[] params = ctx.getParameters();
        String name = (String) params[0];
        params[0] = "JbossSpecIntercepted" + name;
        ctx.setParameters(params);
        Object res = ctx.proceed();
        return res;
    }
}

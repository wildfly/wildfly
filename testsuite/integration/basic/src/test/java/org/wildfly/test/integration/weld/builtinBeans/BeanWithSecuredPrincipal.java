package org.wildfly.test.integration.weld.builtinBeans;

import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.RunAsPrincipal;

@RunAs("Admin")
@RunAsPrincipal("non-anonymous")
@Stateless
public class BeanWithSecuredPrincipal {

    @Resource
    private EJBContext ctx;

    public String getPrincipalName() {
        return ctx.getCallerPrincipal().getName();
    }
}

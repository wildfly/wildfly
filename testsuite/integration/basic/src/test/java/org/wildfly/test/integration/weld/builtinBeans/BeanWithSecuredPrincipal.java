package org.wildfly.test.integration.weld.builtinBeans;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Stateless;

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

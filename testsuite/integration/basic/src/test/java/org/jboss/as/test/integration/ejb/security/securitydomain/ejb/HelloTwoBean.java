package org.jboss.as.test.integration.ejb.security.securitydomain.ejb;

import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;

@Stateless(name="HelloTwoBean")
@Remote(Hello.class)
@PermitAll
public class HelloTwoBean extends BaseHello implements Hello {

    public HelloTwoBean() {
        // HelloTwoBean uses MyNonValidatingSecurityDomain which uses the loginmodule.custom.MyPrincipal
        super("loginmodule.custom.MyPrincipal");
    }

    @EJB(beanName="HelloOneBean")
    private Hello hello1;

    @Override
    protected Hello getOtherEJB() {
        return hello1;
    }
}
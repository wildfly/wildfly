package org.jboss.as.test.integration.ejb.security.securitydomain.ejb;

import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;

@Stateless(name="HelloOneBean")
@Remote(Hello.class)
@PermitAll
public class HelloOneBean extends BaseHello implements Hello {

    public HelloOneBean() {
        // HelloOneBean uses MySecurityDomain which uses the default org.jboss.security.SimplePrincipal
        super(null);
    }

    @EJB(beanName="HelloTwoBean")
    private Hello hello2;

    @Override
    protected Hello getOtherEJB() {
        return hello2;
    }
}
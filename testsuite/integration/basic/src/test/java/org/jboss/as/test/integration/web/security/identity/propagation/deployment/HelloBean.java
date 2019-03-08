package org.jboss.as.test.integration.web.security.identity.propagation.deployment;

import org.jboss.ejb3.annotation.SecurityDomain;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Remote;
import javax.ejb.Stateless;

@Stateless(name="Hello")
@Remote(Hello.class)
@SecurityDomain("auth-test")
public class HelloBean implements Hello {

    @RolesAllowed({ "guest" })
    public void call() {
    }
}

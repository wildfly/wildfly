package org.jboss.as.test.integration.ejb.security;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup({EjbSecurityDomainSetup.class})
@Category(CommonCriteria.class)
public class AuthenticationTestCase extends AbstractAuthenticationTestCase {

    @Deployment
    public static Archive<?> deployment() {
        return getDeployment().addClasses(AuthenticationTestCase.class, EjbSecurityDomainSetup.class);
    }

}

package org.jboss.as.test.integration.ejb.security;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup({EjbElytronDomainSetup.class})
@Category(CommonCriteria.class)
public class AuthenticationElytronTestCase extends AbstractAuthenticationTestCase {

    @BeforeClass
    public static void onlyIfElytronPropertySet() {
        Assume.assumeTrue(System.getProperty("elytron") != null);
    }

    @Deployment
    public static Archive<?> deployment() {
        if (System.getProperty("elytron") == null) { // blank archive (cannot skip)
            return ShrinkWrap.create(WebArchive.class, "ejb3security.war");
        }
        return getDeployment().addClasses(AuthenticationElytronTestCase.class, EjbElytronDomainSetup.class);
    }

}

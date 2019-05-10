package org.jboss.as.test.integration.ejb.jndi.logging;

import javax.ejb.Remote;
import javax.ejb.Stateless;

@Stateless(name="Hello")
@Remote(Hello.class)
public class HelloBean implements Hello {
}

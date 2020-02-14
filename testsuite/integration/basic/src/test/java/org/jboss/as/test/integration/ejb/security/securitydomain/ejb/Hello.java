package org.jboss.as.test.integration.ejb.security.securitydomain.ejb;

import javax.ejb.Remote;
import java.util.List;

@Remote
public interface Hello {

    public static final String HelloOne = "ejb:/EJBContextMultipleSDTestCase/HelloOneBean!org.jboss.as.test.integration.ejb.security.securitydomain.ejb.Hello";
    public static final String HelloTwo = "ejb:/EJBContextMultipleSDTestCase/HelloTwoBean!org.jboss.as.test.integration.ejb.security.securitydomain.ejb.Hello";

    public Info sayHello(Info info);

    public List<String> sayHelloSeveralTimes(Info info);
}

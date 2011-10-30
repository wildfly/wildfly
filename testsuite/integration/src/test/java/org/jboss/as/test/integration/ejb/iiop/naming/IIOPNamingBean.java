package org.jboss.as.test.integration.ejb.iiop.naming;

import javax.ejb.RemoteHome;
import javax.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@RemoteHome(IIOPNamingHome.class)
@Stateless
public class IIOPNamingBean {

    public String hello() {
        return "hello";
    }

}

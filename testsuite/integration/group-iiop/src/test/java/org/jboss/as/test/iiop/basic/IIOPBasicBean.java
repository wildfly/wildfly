package org.jboss.as.test.iiop.basic;

import javax.ejb.RemoteHome;
import javax.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@RemoteHome(IIOPBasicHome.class)
@Stateless
public class IIOPBasicBean {

    public String hello() {
        return "hello";
    }

}

package org.jboss.as.test.iiop.basic;

import javax.ejb.RemoteHome;
import javax.ejb.Stateful;
import javax.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@RemoteHome(IIOPBasicStatefulHome.class)
@Stateful
public class IIOPBasicStatefulBean {

    private int state;

    public void ejbCreate( int state) {
        this.state = state;
    }

    public int state() {
        return state;
    }

}

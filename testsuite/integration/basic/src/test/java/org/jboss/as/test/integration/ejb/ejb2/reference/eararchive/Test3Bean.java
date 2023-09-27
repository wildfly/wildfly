/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.eararchive;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBs;
import jakarta.ejb.Remote;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.Stateless;
import javax.naming.InitialContext;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateless(name = "Test3")
@Remote(Test3Business.class)
@RemoteHome(Test3Home.class)
@EJBs({@EJB(name = "injected/Test2", lookup = "java:app/multideploy-ejb2/ejb_Test2!org.jboss.as.test.integration.ejb.ejb2.reference.eararchive.Test2Home", beanInterface = Test2Home.class)})
public class Test3Bean implements Test3Business {
    private static final Logger log = Logger.getLogger(Test3Bean.class);

    @EJB(name = "ejb/Test2")
    private Test2Home test2Home = null;

    public void testAccess() throws Exception {
        Test2 test2 = test2Home.create();

        InitialContext jndiContext = new InitialContext();
        Test2Home home = (Test2Home) jndiContext.lookup("java:comp/env/injected/Test2");
        test2 = home.create();
    }
}

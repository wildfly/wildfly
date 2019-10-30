/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.eararchive;

import javax.ejb.EJB;
import javax.ejb.EJBs;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.Stateless;
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

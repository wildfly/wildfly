/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.demos.managedbean.mbean;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.demos.managedbean.archive.SimpleManagedBean;
import org.jboss.logging.Logger;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class Test implements TestMBean {

    Logger log = Logger.getLogger(Test.class.getName());

    @Override
    public void test() {
        log.info("In test()");
        try {
            InitialContext ctx = new InitialContext();
            Object o = ctx.lookup("global/managedbean-example_jar/SimpleManagedBean");
            System.out.println("-----> Found " + o);
            SimpleManagedBean bean = (SimpleManagedBean)o;
            System.out.println("Cast to SimpleManagedBean");
        } catch (NamingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void start() {
        log.info("Starting MBean " + this.getClass().getName());
    }
}

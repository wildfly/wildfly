/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.domain.mixed;

import java.net.URL;
import java.net.URLConnection;

import org.jboss.as.test.integration.domain.mixed.util.AbstractMixedDomainTest;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class SimpleMixedDomainTest extends AbstractMixedDomainTest {

    private final String slaveAddress = System.getProperty("jboss.test.host.slave.address", "localhost");


    @Test
    public void testServerRunning() throws Exception {
        URLConnection connection = new URL("http://" + TestSuiteEnvironment.formatPossibleIpv6Address(slaveAddress) + ":8080").openConnection();
        connection.connect();

    }

}

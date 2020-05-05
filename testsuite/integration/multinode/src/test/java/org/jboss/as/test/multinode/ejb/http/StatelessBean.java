/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.multinode.ejb.http;

import org.jboss.logging.Logger;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

@Stateless
@Local(StatelessLocal.class)
@Remote(StatelessRemote.class)
public class StatelessBean {
    private static final Logger log = Logger.getLogger(StatelessBean.class);

    private static int methodCount = 0;

    private InitialContext getInitialContext() throws NamingException {
        final Properties props = new Properties();
        // setup the ejb: namespace URL factory
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        return new InitialContext(props);
    }

    public int remoteCall() throws Exception {
        ++methodCount;
        InitialContext jndiContext = getInitialContext();
        log.trace("Calling Remote... " + jndiContext.getEnvironment());
        StatelessRemote stateless = (StatelessRemote) jndiContext.lookup("ejb:/" +EjbOverHttpTestCase.ARCHIVE_NAME_SERVER
                + "//" + StatelessBean.class.getSimpleName() + "!" + StatelessRemote.class.getName());
        return stateless.method();
    }

    public int method() throws Exception {
        ++methodCount;
        log.trace("Method called " + methodCount);
        return methodCount;
    }
}

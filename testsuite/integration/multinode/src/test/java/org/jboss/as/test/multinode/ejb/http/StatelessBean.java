/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.ejb.http;

import org.jboss.logging.Logger;

import jakarta.ejb.Local;
import jakarta.ejb.NoSuchEJBException;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

@Stateless
@Local(StatelessLocal.class)
@Remote(StatelessRemote.class)
public class StatelessBean {
    private static final Logger log = Logger.getLogger(StatelessBean.class);

    private int methodCount = 0;

    private InitialContext getInitialContext() throws NamingException {
        final Properties props = new Properties();
        // setup the Jakarta Enterprise Beans: namespace URL factory
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        return new InitialContext(props);
    }

    public int remoteCall() throws Exception {
        ++methodCount;
        InitialContext jndiContext = getInitialContext();
        log.trace("Calling Remote... " + jndiContext.getEnvironment());
        StatelessRemote stateless = (StatelessRemote) jndiContext.lookup("ejb:/" +EjbOverHttpTestCase.ARCHIVE_NAME_SERVER
                + "//" + StatelessBean.class.getSimpleName() + "!" + StatelessRemote.class.getName());
        try {
            return stateless.method();
        } catch (NoSuchEJBException e) {
            return EjbOverHttpTestCase.NO_EJB_RETURN_CODE;
        }
    }

    public int method() throws Exception {
        ++methodCount;
        log.trace("Method called " + methodCount);
        return methodCount;
    }
}

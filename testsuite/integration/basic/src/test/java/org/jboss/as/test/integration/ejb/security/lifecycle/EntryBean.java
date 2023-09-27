/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.lifecycle;

import java.util.Map;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * The main bean to call the beans being tested and return the results.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Stateless
@SecurityDomain("ejb3-tests")
public class EntryBean {

    @Resource
    private SessionContext sessionContext;

    public Map<String, String> testStatefulBean() {
        return testSessionBean("java:global/ejb3security/StatefulBean");
    }

    public Map<String, String> testStatlessBean() {
        return testSessionBean("java:global/ejb3security/StatelessBean");
    }

    private Map<String, String> testSessionBean(final String jndiName) {
        ResultHolder.reset();
        SessionBean sessionBean = (SessionBean) sessionContext.lookup(jndiName);

        sessionBean.business();
        sessionBean.remove();

        return ResultHolder.getResults();
    }

}

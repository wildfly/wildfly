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
package org.jboss.as.test.integration.ejb.security.lifecycle;

import java.util.Map;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

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

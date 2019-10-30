/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ee.injection.resource.basic;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TimerService;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;
import java.net.URL;

/**
 * User: jpai
 */
@Stateless
public class SimpleSLSB extends Parent {

    // injected via the setter
    private Parent otherBean;

    @Resource
    private TimerService timerService;

    @Resource
    private SessionContext sessionContext;

    @Resource(name = "simpleString")
    private String simpleStringFromDeploymentDescriptor;

    public static final int DEFAULT_UNINJECTED_INT_VAL = 4;

    public static final String DEFAULT_UNINJECTED_STRING_VAL = "This is the default value!!!! ###";

    @Resource(name = "missingEnvEntryValIntResource")
    private int wontBeInjected = DEFAULT_UNINJECTED_INT_VAL;

    // @Resource is used on the setter
    private String wontBeInjectedString = DEFAULT_UNINJECTED_STRING_VAL;

    @Resource(name = "url1")
    private URL url1;

    @Resource(lookup = "http://jboss.org")
    private URL url2;

    @Resource(lookup = "file://dev/null")
    private Object url3;

    @Resource(lookup = "java:comp/env/url1")
    private URL url4;

    @Resource(name = "ldapContext1")
    private LdapContext ldapContext1;

    @Resource(lookup = "ldap://localhost:389")
    private DirContext ldapContext2;

    @Resource(lookup = "java:comp/env")
    private Context context1;

    @Resource(lookup = "java:comp/null")
    private Context context2;

    public String sayHello(String user) {
        return this.commonBean.sayHello(user);
    }

    public Class<?> getInvokedBusinessInterface() {
        SessionContext sessionContext = (SessionContext) this.ejbContext;
        return sessionContext.getInvokedBusinessInterface();
    }

    @EJB(beanInterface = OtherSLSB.class)
    public void setOtherBean(Parent otherBean) {
        this.otherBean = otherBean;
    }

    public String getInjectedString() {
        return this.simpleStringFromDeploymentDescriptor;
    }

    public int getUnInjectedInt() {
        return this.wontBeInjected;
    }

    @Resource(name = "missingEnvEntryValStringResource")
    public void setUnInjectedString(String val) {
        this.wontBeInjectedString = val;
    }

    public String getUnInjectedString() {
        return this.wontBeInjectedString;
    }

    public boolean isUnInjectedIntEnvEntryPresentInEnc() {
        Context ctx = null;
        try {
            ctx = new InitialContext();
            ctx.lookup("java:comp/env/missingEnvEntryValIntResource");
            return true;
        } catch (NameNotFoundException nnfe) {
            return false;
        } catch (NamingException ne) {
            throw new RuntimeException(ne);
        }
    }

    public boolean isUnInjectedStringEnvEntryPresentInEnc() {
        Context ctx = null;
        try {
            ctx = new InitialContext();
            ctx.lookup("java:comp/env/missingEnvEntryValStringResource");
            return true;
        } catch (NameNotFoundException nnfe) {
            return false;
        } catch (NamingException ne) {
            throw new RuntimeException(ne);
        }
    }

    public boolean isTimerServiceInjected() {
        return this.timerService != null;
    }

    public boolean validURLInjections() {
        return this.url1 != null && this.url2 != null && url3 != null && url4 != null;
    }

    public boolean validContextInjections() {
        return this.context1 != null && this.context2 == null;
    }
}

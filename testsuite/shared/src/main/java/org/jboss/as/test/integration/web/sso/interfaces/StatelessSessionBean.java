/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.web.sso.interfaces;

import java.security.Principal;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.jboss.logging.Logger;


/**
 * A simple session bean for testing declarative security.
 *
 * @author Scott.Stark@jboss.org
 */
public class StatelessSessionBean implements SessionBean {

    private static final long serialVersionUID = -4565135285688543978L;

    static Logger log = Logger.getLogger(StatelessSessionBean.class);

    private SessionContext sessionContext;

    public void ejbCreate() throws CreateException {
        log.debug("ejbCreate() called");
    }

    public void ejbActivate() {
        log.debug("ejbActivate() called");
    }

    public void ejbPassivate() {
        log.debug("ejbPassivate() called");
    }

    public void ejbRemove() {
        log.debug("ejbRemove() called");
    }

    public void setSessionContext(SessionContext context) {
        log.debug("setSessionContext() called");
        sessionContext = context;
    }

    public String echo(String arg) {
        log.debug("echo, arg=" + arg);
        Principal p = sessionContext.getCallerPrincipal();
        log.debug("echo, callerPrincipal=" + p);
        return p.getName();
    }

    public void noop() {
        log.debug("noop");
    }

    public ReturnData getData() {
        ReturnData data = new ReturnData();
        data.data = "TheReturnData";
        return data;
    }

}

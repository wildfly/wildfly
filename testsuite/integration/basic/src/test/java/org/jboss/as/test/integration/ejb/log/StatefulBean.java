/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.ejb.log;

import org.apache.log4j.Logger;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

public class StatefulBean implements SessionBean, StatefulInterface {

    protected SessionContext context;
    protected Logger log = Logger.getLogger( this.getClass() );

    public void ejbCreate() throws CreateException {
        log.info("ejbCreate();");
    }

    public void ejbRemove() {
        log.info("ejbRemove();");
    }

    public void ejbActivate() {
        log.info("ejbActivate();");
    }

    public void ejbPassivate() {
        log.info("ejbPassivate();");
    }

    public void setSessionContext(SessionContext context) {
        this.context = context;
    }
}

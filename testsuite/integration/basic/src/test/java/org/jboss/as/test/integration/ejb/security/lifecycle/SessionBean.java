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

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Remove;
import javax.ejb.SessionContext;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class SessionBean extends BaseBean {

    private SessionContext sessionContext;

    public EJBContext getEJBContext() {
        return sessionContext;
    }

    // TODO - Why do I need to override this method.
    public void performTests(final String beanMethod) {
        super.performTests(beanMethod);
    }

    @Resource
    public void setSessionContext(SessionContext sessionContext) {
        this.sessionContext = sessionContext;
        performTests(DEPENDENCY_INJECTION);
    }

    @PostConstruct
    public void postConstruct() {
        performTests(LIFECYCLE_CALLBACK);
    }

    public void business() {
        performTests(BUSINESS);
    }

    @Remove
    public void remove() {
    }

    // TODO - Work out how to fit in afterCompletion.

}

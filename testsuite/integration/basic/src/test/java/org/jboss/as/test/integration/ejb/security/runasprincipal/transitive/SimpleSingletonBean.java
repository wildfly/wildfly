/*
 * Copyright (C) 2013 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file
 * in the distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.test.integration.ejb.security.runasprincipal.transitive;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import org.jboss.as.test.integration.ejb.security.runasprincipal.WhoAmI;
import org.jboss.ejb3.annotation.SecurityDomain;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
@Singleton
@Remote(WhoAmI.class)
@SecurityDomain("other")
public class SimpleSingletonBean implements WhoAmI {

    @EJB(beanName = "StatelessBBean")
    private WhoAmI beanB;

    private String principal;

    @PostConstruct
    public void init() {
        principal = beanB.getCallerPrincipal();
    }

    public String getCallerPrincipal() {
        return principal;
    }

}

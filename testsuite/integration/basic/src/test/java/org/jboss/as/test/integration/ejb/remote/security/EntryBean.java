/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.remote.security;

import org.jboss.as.test.shared.integration.ejb.security.Util;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Remote;
import javax.ejb.Stateless;

/**
 * An unsecured EJB used to test switching the identity before calling a secured EJB.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Stateless
@Remote(IntermediateAccess.class)
public class EntryBean implements IntermediateAccess {

    @Resource
    private EJBContext context;

    @EJB
    private SecurityInformation ejb;

    @Override
    public String getPrincipalName() {
        return context.getCallerPrincipal().getName();
    }

    @Override
    public String getPrincipalName(String username, String password) {
        try {
            return Util.switchIdentity(username, password, () -> ejb.getPrincipalName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

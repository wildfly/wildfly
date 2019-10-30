/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.singleton.dependson.mdb;

import javax.ejb.Singleton;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Proxy which is deployed with MDB.
 *
 * @author baranowb
 */
@Singleton
public class CallCounterProxy {

    /**
     * @param postConstruct the postConstruct to set
     */
    public void setPostConstruct() {
        this.getCounter().setPostConstruct();
    }

    /**
     * @param preDestroy the preDestroy to set
     */
    public void setPreDestroy() {
        this.getCounter().setPreDestroy();
    }

    /**
     *
     */
    public void setMessage() {
        this.getCounter().setMessage();
    }

    private CallCounterInterface getCounter() {
        InitialContext ctx = null;
        try {
            ctx = new InitialContext();
            return (CallCounterInterface) ctx.lookup(Constants.SINGLETON_EJB);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                }
            }
        }
    }
}

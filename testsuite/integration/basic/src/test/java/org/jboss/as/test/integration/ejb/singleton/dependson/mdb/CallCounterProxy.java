/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.dependson.mdb;

import jakarta.ejb.Singleton;
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

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.dependson.mdb;

import jakarta.ejb.Singleton;

/**
 * Counting ordering of calls.
 *
 * @author baranowb
 */
@Singleton
public class CallCounterSingleton implements CallCounterInterface {
    private boolean postConstruct, preDestroy, message;

    /**
     * @return the postConstruct
     */
    public boolean isPostConstruct() {
        return postConstruct;
    }

    /**
     * @param postConstruct the postConstruct to set
     */
    public void setPostConstruct() {
        this.postConstruct = true;
    }

    /**
     * @return the preDestroy
     */
    public boolean isPreDestroy() {
        return preDestroy;
    }

    /**
     * @param preDestroy the preDestroy to set
     */
    public void setPreDestroy() {
        this.preDestroy = true;
    }

    /**
     *
     */
    public void setMessage() {
        this.message = true;
    }

    /**
     * @return the message
     */
    public boolean isMessage() {
        return message;
    }

}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.callerprincipal;

import java.util.HashMap;
import java.util.Map;

import jakarta.ejb.Remote;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

@Singleton
@Startup
@Remote(ITestResultsSingleton.class)
public class TestResultsSingleton implements ITestResultsSingleton {

    private Map<String, String> slsb = new HashMap<String, String>();
    private Map<String, String> sfsb = new HashMap<String, String>();
    private Map<String, String> mdb = new HashMap<String, String>();
    private Map<String, String> eb = new HashMap<String, String>();

    public String getSlsb(String index) {
        return this.slsb.get(index);
    }
    public void setSlsb(String index, String value) {
        this.slsb.put(index, value);
    }
    public String getSfsb(String index) {
        return this.sfsb.get(index);
    }
    public void setSfsb(String index, String value) {
        this.sfsb.put(index,  value);
    }
    public String getMdb(String index) {
        return mdb.get(index);
    }
    public void setMdb(String index, String value) {
        this.mdb.put(index, value);
    }
    public String getEb(String index) {
        return eb.get(index);
    }
    public void setEb(String index, String value) {
        this.eb.put(index, value);
    }
}

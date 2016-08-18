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

package org.jboss.as.test.integration.ejb.security.callerprincipal;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;

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

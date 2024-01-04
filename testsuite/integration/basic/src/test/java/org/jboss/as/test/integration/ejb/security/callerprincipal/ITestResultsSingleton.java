/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.callerprincipal;

public interface ITestResultsSingleton {
    String getSlsb(String index);
    void setSlsb(String index, String value);
    String getSfsb(String index);
    void setSfsb(String index, String value);
    String getMdb(String index);
    void setMdb(String index, String value);
    String getEb(String index);
    void setEb(String index, String value);
}

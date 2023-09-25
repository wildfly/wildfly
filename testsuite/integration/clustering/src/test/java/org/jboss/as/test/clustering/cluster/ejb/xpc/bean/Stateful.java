/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.xpc.bean;

import jakarta.ejb.Remove;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Interface to see if that helps avoid the
 * error:
 * <p/>
 * WFLYEJB0034: EJB Invocation failed on component StatefulBean for method
 * public org.jboss.as.test.clustering.unmanaged.ejb3.xpc.bean.Employee
 * org.jboss.as.test.clustering.unmanaged.ejb3.xpc.bean.StatefulBean.getEmployee(int):
 * java.lang.IllegalArgumentException: object is not an instance of declaring class
 *
 * @author Scott Marlow
 */
public interface Stateful {
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    void createEmployee(String name, String address, int id);

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    Employee getEmployee(int id);

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    Employee getSecondBeanEmployee(int id);

    @Remove
    void destroy();

    void flush();

    void clear();

    void deleteEmployee(int id);

    void echo(String message);

    int executeNativeSQL(String nativeSql);

    String getVersion();

    long getEmployeesInMemory();
}

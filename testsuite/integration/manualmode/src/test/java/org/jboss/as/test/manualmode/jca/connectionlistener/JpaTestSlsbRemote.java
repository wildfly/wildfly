/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.jca.connectionlistener;

import java.sql.SQLException;

import jakarta.ejb.Remote;

/**
 * @author <a href="mailto:hsvabek@redhat.com">Hynek Svabek</a>
 */
@Remote
public interface JpaTestSlsbRemote {

    void initDataSource(boolean useXaDs);

    void insertRecord() throws SQLException;

    void insertRecord(boolean doRollback) throws SQLException;

    void assertRecords(int expectedRecords) throws SQLException;

    void assertExactCountOfRecords(int expectedRecords, int expectedActivated, int expectedPassivated) throws SQLException;
}

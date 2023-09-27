/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.datasource;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Test JDBC driver
 */
public class TestDriver2 implements Driver {
  /**
   * {@inheritDoc}
   */
  public Connection connect(String url, Properties info) throws SQLException {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public boolean acceptsURL(String url) throws SQLException {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
    Driver driver = DriverManager.getDriver(url);
    return driver.getPropertyInfo(url, info);
  }

  /**
   * {@inheritDoc}
   */
  public int getMajorVersion() {
    return 1;
  }

  /**
   * {@inheritDoc}
   */
  public int getMinorVersion() {
    return 1;
  }

  /**
   * {@inheritDoc}
   */
  public boolean jdbcCompliant() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    throw new SQLFeatureNotSupportedException();
  }
}

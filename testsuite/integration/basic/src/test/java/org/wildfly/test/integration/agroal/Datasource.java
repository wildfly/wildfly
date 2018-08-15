/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.agroal;

/**
 * @author <a href="mailto:ochaloup@redhat.com">Ondra Chaloupka</a>
 */
public final class Datasource {
    private String name, jndiName, driverName, driverModule, connectionUrl, userName, password, driverClass;

    private int minSize, initialSize, maxSize, blockingTimeout;

    private Datasource(Builder builder) {
        this.name = builder.datasourceName;
        this.jndiName = builder.jndiName;
        this.driverName = builder.driverName;
        this.driverModule = builder.driverModule;
        this.connectionUrl = builder.connectionUrl;
        this.userName = builder.userName;
        this.password = builder.password;
        this.driverClass = builder.driverClass;

        this.minSize = builder.minSize;
        this.initialSize = builder.initialSize;
        this.maxSize = builder.maxSize;
        this.blockingTimeout = builder.blockingTimeout;
    }

    public static Builder Builder(String datasourceName) {
        return new Builder(datasourceName);
    }

    public String getName() {
        return name;
    }

    public String getJndiName() {
        return jndiName;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getDriverModule() {
        return driverModule;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public int getMinSize() {
        return minSize;
    }

    public int getInitialSize() {
        return initialSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getBlockingTimeout() {
        return blockingTimeout;
    }

    @Override
    public String toString() {
        return String.format("Datasource name: %s, jndi: %s, driver name: %s, driverModule %s, url: %s, user name: %s, password: %s, datasource-class: %s",
                name, jndiName, driverName, driverModule, connectionUrl, userName, password, driverClass);
    }

    public static final class Builder {
        private final String datasourceName;
        private String jndiName;
        private String driverName = System.getProperty("ds.jdbc.driver");
        private String driverModule = System.getProperty("ds.jdbc.driver.module");
        private String driverClass = System.getProperty("ds.jdbc.driver.class");
        private String connectionUrl = System.getProperty("ds.jdbc.url");
        private String userName = System.getProperty("ds.jdbc.user");
        private String password = System.getProperty("ds.jdbc.pass");

        private int minSize, initialSize, maxSize, blockingTimeout;

        private Builder(String datasourceName) {
            this.datasourceName = datasourceName;
            this.jndiName = "java:jboss/datasources/" + datasourceName;
            if (this.driverName == null) {
                driverName = "h2";
            }
            if (this.driverModule == null) {
                driverModule = "com.h2database.h2";
            }
            if (this.driverClass == null) {
                driverClass = "org.h2.jdbcx.JdbcDataSource";
            }
            if (this.connectionUrl == null) {
                connectionUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
            }
            if (this.userName == null) {
                userName = "sa";
            }
            if (this.password == null) {
                password = "sa";
            }

            maxSize = 10;
        }

        public Builder jndiName(String jndiName) {
            this.jndiName = jndiName;
            return this;
        }

        public Builder driverName(String driverName) {
            this.driverName = driverName;
            return this;
        }

        public Builder driverModule(String driverModule) {
            this.driverModule = driverModule;
            return this;
        }

        public Builder driverClass(String driverClass) {
            this.driverClass = driverClass;
            return this;
        }

        public Builder connectionUrl(String connectionUrl) {
            this.connectionUrl = connectionUrl;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder minSize(int minSize) {
            this.minSize = minSize;
            return this;
        }

        public Builder initialSize(int initialSize) {
            this.initialSize = initialSize;
            return this;
        }

        public Builder maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder blockingTimeout(int blockingTimeout) {
            this.blockingTimeout = blockingTimeout;
            return this;
        }

        public Datasource build() {
            return new Datasource(this);
        }
    }
}

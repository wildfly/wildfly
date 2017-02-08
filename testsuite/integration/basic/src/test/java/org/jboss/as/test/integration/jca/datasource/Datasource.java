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

package org.jboss.as.test.integration.jca.datasource;

/**
 * @author <a href="mailto:ochaloup@redhat.com>Ondra Chaloupka</a>
 */
public class Datasource {
    private final String name, jndiName, driverName, connectionUrl, userName, password, enabled, dataSourceClass;

    private Datasource(Builder builder) {
        this.name = builder.datasourceName;
        this.jndiName = builder.jndiName;
        this.driverName = builder.driverName;
        this.connectionUrl = builder.connectionUrl;
        this.userName = builder.userName;
        this.password = builder.password;
        this.enabled = builder.enabled;
        this.dataSourceClass = builder.dataSourceClass;
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

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getEnabled() {
        return enabled;
    }

    public String getDataSourceClass() {
        return dataSourceClass;
    }

    @Override
    public String toString() {
        return String.format("Datasource name: %s, jndi: %s, driver name: %s, url: %s, user name: %s, password: %s, enabled: %s, datasource-class: %s",
                name, jndiName, driverName, connectionUrl, userName, password, enabled, dataSourceClass);
    }


    public static final class Builder {
        private final String datasourceName;
        private String jndiName;
        private String enabled = "true";
        private String driverName = System.getProperty("ds.jdbc.driver");
        private String connectionUrl = System.getProperty("ds.jdbc.url");
        private String userName = System.getProperty("ds.jdbc.user");
        private String password = System.getProperty("ds.jdbc.pass");
        private String dataSourceClass;

        private Builder(String datasourceName) {
            this.datasourceName = datasourceName;
            this.jndiName = "java:jboss/datasources/" + datasourceName;
            if (this.driverName == null) { driverName = "h2"; }
            if (this.connectionUrl == null) { connectionUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"; }
            if (this.userName == null) { userName = "sa"; }
            if (this.password == null) { password = "sa"; }
        }

        public Builder jndiName(String jndiName) {
            this.jndiName = jndiName;
            return this;
        }

        public Builder driverName(String driverName) {
            this.driverName = driverName;
            return this;
        }

        public Builder enabled(Boolean enabled) {
            this.enabled = enabled.toString();
            return this;
        }

        public Builder enabled(String enabled) {
            this.enabled = enabled;
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

        public Builder dataSourceClass(String dataSourceClass) {
            this.dataSourceClass = dataSourceClass;
            return this;
        }

        public Datasource build() {
            return new Datasource(this);
        }
    }
}

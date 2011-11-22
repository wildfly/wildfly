/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxr.service;

import org.jboss.as.jaxr.extension.JAXRConstants;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.naming.CompositeName;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The JUDDI Service for the JUDDI open source project
 * <p/>
 * Original source at https://svn.jboss.org/repos/jbossas/projects/jaxr/tags/2.0.2
 *
 * @author Anil.Saldhana@jboss.com
 * @author Thomas.Diesler@jboss.com
 * @since 26-Oct-2011
 */
public final class JAXRDatasourceService extends AbstractService<Void> {

    static final ServiceName SERVICE_NAME = JAXRConstants.SERVICE_BASE_NAME.append("datasource");

    // [TODO] AS7-2277 JAXR subsystem i18n
    private final Logger log = Logger.getLogger(JAXRDatasourceService.class);

    private final InjectedValue<NamingStore> injectedJavaContext = new InjectedValue<NamingStore>();
    private final InjectedValue<JAXRConfiguration> injectedConfig = new InjectedValue<JAXRConfiguration>();
    private DataSource datasource;

    public static ServiceController<?> addService(final ServiceTarget target, final ServiceListener<Object>... listeners) {
        JAXRDatasourceService service = new JAXRDatasourceService();
        ServiceBuilder<?> builder = target.addService(SERVICE_NAME, service);
        builder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, NamingStore.class, service.injectedJavaContext);
        builder.addDependency(JAXRConfigurationService.SERVICE_NAME, JAXRConfiguration.class, service.injectedConfig);
        builder.addListener(listeners);
        return builder.install();
    }

    // Hide ctor
    private JAXRDatasourceService() {
    }

    @Override
    public void start(final StartContext context) throws StartException {
        final JAXRConfiguration config = injectedConfig.getValue();
        if (config.getDataSourceBinding() != null) {
            final ServiceTarget childTarget = context.getChildTarget();
            final ServiceBuilder<Void> builder = childTarget.addService(SERVICE_NAME.append("runner"), new AbstractService<Void>() {
                @Override
                public void start(StartContext context) throws StartException {
                    log.infof("Starting: %s", getClass().getSimpleName());
                    try {
                        NamingStore namingStore = injectedJavaContext.getValue();
                        String lookup = config.getDataSourceBinding();
                        if (lookup == null || lookup.startsWith("java:") == false)
                            throw new IllegalStateException("Datasource lookup expected in java context: %s" + lookup);

                        // [TODO] AS7-2302 Potential race condition on DataSource lookup
                        long timeout = 2000;
                        while (datasource == null && timeout > 0) {
                            try {
                                datasource = (DataSource) namingStore.lookup(new CompositeName(lookup.substring(5)));
                            } catch (Exception ex) {
                                Thread.sleep(200);
                                timeout -= 200;
                            }
                        }
                        if (datasource == null)
                            throw new IllegalStateException("Cannot obtain data source: " + lookup);

                        if (config.isDropOnStart()) {
                            log.debug("Drop juddi tables on start");
                            locateAndRunScript("juddi_drop_db.ddl", Level.DEBUG);
                        }
                        if (config.isCreateOnStart()) {
                            log.debug("Create juddi tables on start");
                            locateAndRunScript("juddi_create_db.ddl", Level.ERROR);
                            locateAndRunScript("juddi_data.ddl", Level.ERROR);
                        }
                    } catch (Exception ex) {
                        log.errorf(ex, "Cannot start JUDDI service");
                    }
                }

                @Override
                public void stop(StopContext context) {
                    log.infof("Stopping: %s", getClass().getSimpleName());
                    try {
                        JAXRConfiguration config = injectedConfig.getValue();
                        if (config.isDropOnStop()) {
                            try {
                                log.debug("Drop juddi tables on sop");
                                locateAndRunScript("juddi_drop_db.ddl", Level.DEBUG);
                            } catch (SQLException ex) {
                                // [TODO] AS7-2572 Cannot run JAXR drop tables script on shutdown
                                log.errorf("Cannot drop JAXR tables: %s", ex);
                            }
                        }
                    } catch (Exception ex) {
                        log.errorf(ex, "Failure stopping JUDDI service");
                    }
                }
            });
            builder.addDependency(ServiceName.JBOSS.append("data-source", config.getDataSourceBinding()));
            builder.install();
        }
    }

    private void locateAndRunScript(String name, Level errorlevel) throws SQLException, IOException {
        log.infof("RunScript: %s", name);
        InputStream input = getClass().getClassLoader().getResourceAsStream("META-INF/ddl/" + name);
        if (input == null) {
            log.errorf("Cannot load DDL script: %s", name);
            return;
        }
        try {
            runScript(input, errorlevel);
        } finally {
            input.close();
        }
    }

    private void runScript(InputStream stream, Level errorlevel) throws SQLException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        Connection connection = datasource.getConnection();
        if (connection == null) {
            log.errorf("Cannot obtain connection");
            return;
        }
        try {
            String nextLine;
            StringBuffer nextStatement = new StringBuffer();
            while ((nextLine = reader.readLine()) != null) {
                nextLine = nextLine.trim();
                if (nextLine.startsWith("--")) {
                    continue;
                }
                nextStatement.append(nextLine);
                if (nextLine.endsWith(";")) {
                    String sqlStatement = nextStatement.substring(0, nextStatement.indexOf(";"));
                    log.debugf("Statement to execute: '%s'", sqlStatement);
                    try {
                        Statement statement = connection.createStatement();
                        statement.execute(sqlStatement);
                        statement.close();
                    } catch (SQLException e) {
                        String msg = "Could not execute statement: %s";
                        log.logf(errorlevel, msg, e.getLocalizedMessage());
                    } finally {
                        nextStatement = new StringBuffer();
                    }
                }
            }
        } finally {
            connection.close();
        }
    }
}

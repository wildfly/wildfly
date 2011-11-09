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

import org.apache.ws.scout.registry.ConnectionFactoryImpl;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
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
public final class JAXRBootstrapService extends AbstractService<Void> {

    static final ServiceName SERVICE_NAME = JAXRConfiguration.SERVICE_BASE_NAME.append("bootstrap");

    // [TODO] AS7-2277 JAXR subsystem i18n
    private final Logger log = Logger.getLogger(JAXRBootstrapService.class);

    private final InjectedValue<NamingStore> injectedJavaContext = new InjectedValue<NamingStore>();
    private final JAXRConfiguration config;
    private DataSource datasource;

    public static ServiceController<?> addService(final ServiceTarget target, final JAXRConfiguration config, final ServiceListener<Object>... listeners) {
        JAXRBootstrapService service = new JAXRBootstrapService(config);
        ServiceBuilder<?> builder = target.addService(SERVICE_NAME, service);
        builder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, NamingStore.class, service.injectedJavaContext);
        builder.addDependency(ServiceName.JBOSS.append("data-source", config.getDataSourceBinding()));
        builder.addListener(listeners);
        return builder.install();
    }

    private JAXRBootstrapService(JAXRConfiguration config) {
        this.config = config;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        log.infof("Starting JAXRBootstrapService");
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

            if (config.getConnectionFactoryBinding() != null) {
                bindJAXRConnectionFactory(context);
            }
            if (config.isDropOnStart()) {
                runDrop();
            }
            if (config.isCreateOnStart()) {
                runCreate();
            }
        } catch (Exception ex) {
            log.errorf(ex, "Cannot start JUDDI service");
        }
    }

    @Override
    public void stop(final StopContext context) {
        log.infof("Stopping JAXRBootstrapService");
        try {
            unbindJAXRConnectionFactory(context);
            if (config.isDropOnStop()) {
                try {
                    runDrop();
                } catch (SQLException ex) {
                    // [TODO] AS7-2572 Cannot run JAXR drop tables script on shutdown
                    log.errorf("Cannot drop JAXR tables: %s", ex);
                }
            }
        } catch (Exception ex) {
            log.errorf(ex, "Failure stopping JUDDI service");
        }
    }

    private void runDrop() throws SQLException, IOException {
        log.debug("JAXRBootstrapService: Inside runDrop");
        locateAndRunScript("juddi_drop_db.ddl");
        log.debug("JAXRBootstrapService: Exit runDrop");
    }

    private void runCreate() throws SQLException, IOException {
        log.debug("JAXRBootstrapService: Inside runCreate");
        locateAndRunScript("juddi_create_db.ddl");
        locateAndRunScript("juddi_data.ddl");
    }

    private void locateAndRunScript(String name) throws SQLException, IOException {
        log.infof("RunScript: %s", name);
        InputStream input = getClass().getClassLoader().getResourceAsStream("META-INF/ddl/" + name);
        if (input == null) {
            log.errorf("Cannot load DDL script: %s", name);
            return;
        }
        try {
            runScript(input);
        } finally {
            input.close();
        }
    }

    private void runScript(InputStream stream) throws SQLException, IOException {
        boolean firstError = true;
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        Connection connection = null;
        try {
            connection = datasource.getConnection();
            if (connection == null) {
                log.errorf("Cannot obtain connection");
                return;
            }
            Statement statement = connection.createStatement();
            try {
                String nextStatement = "";
                String nextLine;
                while ((nextLine = reader.readLine()) != null) {
                    log.debug("Statement Obtained=" + nextLine);
                    nextLine = nextLine.trim();
                    if (nextLine.indexOf("--") != -1)
                        continue;
                    int semicolon = nextLine.indexOf(";");
                    if (semicolon != -1) {
                        nextStatement += nextLine.substring(0, semicolon);
                        try {
                            log.debug("Statement to execute:" + nextStatement);
                            statement.execute(nextStatement);
                        } catch (SQLException e) {
                            String err = "Could not execute a statement of juddi script::";

                            if (firstError) {
                                log.debug(err + e.getLocalizedMessage() + " " + nextStatement);
                                log.debug("Your settings are:dropOnStart =" + config.isDropOnStart() + ";createOnStart =" + config.isCreateOnStart());
                                log.debug("dropOnStop = " + config.isDropOnStop());

                                firstError = false;
                            }
                        }
                        nextStatement = nextLine.substring(semicolon + 1);
                    } else {
                        nextStatement += nextLine;
                    }
                }
                if (!nextStatement.equals("")) {
                    try {
                        log.debug("Statement to execute:" + nextStatement);
                        statement.execute(nextStatement);
                    } catch (SQLException e) {
                        log.debug("Could not execute last statement of a juddi init script: " + e.getLocalizedMessage());
                        log.debug("Your settings are:dropOnStart =" + config.isDropOnStart() + ";createOnStart =" + config.isCreateOnStart());
                        log.debug("dropOnStop = " + config.isDropOnStop());
                    }
                }
            } finally {
                if (statement != null)
                    statement.close();
            }
        } finally {
            if (connection != null)
                connection.close();
        }
    }


    private void bindJAXRConnectionFactory(StartContext context) {
        log.infof("Binding JAXR ConnectionFactory: %s", config.getConnectionFactoryBinding());
        try {
            String jndiName = config.getConnectionFactoryBinding();
            ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
            BinderService binderService = new BinderService(bindInfo.getBindName());
            ImmediateValue value = new ImmediateValue(new ConnectionFactoryImpl());
            binderService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(value));
            binderService.getNamingStoreInjector().inject((ServiceBasedNamingStore) injectedJavaContext.getValue());
            ServiceBuilder<?> builder = context.getChildTarget().addService(bindInfo.getBinderServiceName(), binderService);
            builder.install();
        } catch (Exception ex) {
            log.errorf(ex, "Cannot bind JAXR ConnectionFactory");
        }
    }

    private void unbindJAXRConnectionFactory(StopContext context) {
        log.debugf("Unbind JAXR ConnectionFactory");
        try {
            String jndiName = config.getConnectionFactoryBinding();
            ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
            ServiceContainer serviceContainer = context.getController().getServiceContainer();
            ServiceController<?> service = serviceContainer.getService(bindInfo.getBinderServiceName());
            if (service != null) {
                service.setMode(ServiceController.Mode.REMOVE);
            }
        } catch (Exception ex) {
            log.errorf(ex, "Cannot unbind JAXR ConnectionFactory");
        }
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.datasources.agroal;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.SimplePassword;
import io.agroal.api.transaction.TransactionIntegration;
import io.agroal.narayana.NarayanaTransactionIntegration;
import org.ietf.jgss.GSSException;
import org.jboss.as.naming.ImmediateManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.extension.datasources.agroal.logging.AgroalLogger;
import org.wildfly.extension.datasources.agroal.logging.LoggingDataSourceListener;
import org.wildfly.security.auth.callback.CredentialCallback;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.AuthenticationContextConfigurationClient;
import org.wildfly.security.auth.principal.NamePrincipal;
import org.wildfly.security.credential.GSSKerberosCredential;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.transaction.client.ContextTransactionManager;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Defines an extension to provide DataSources based on the Agroal project
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class DataSourceService implements Service<AgroalDataSource>, Supplier<AgroalDataSource> {

    private static final AuthenticationContextConfigurationClient AUTH_CONFIG_CLIENT = AccessController.doPrivileged(AuthenticationContextConfigurationClient.ACTION);

    private final Consumer<AgroalDataSource> consumer;
    private final String dataSourceName;
    private final String jndiName;
    private final boolean jta;
    private final boolean connectable;
    private final boolean xa;

    private final AgroalDataSourceConfigurationSupplier dataSourceConfiguration;
    private AgroalDataSource agroalDataSource;

    private final Supplier<Class> driverSupplier;
    private final Supplier<AuthenticationContext> authenticationContextSupplier;
    private final Supplier<ExceptionSupplier<CredentialSource, Exception>> credentialSourceSupplier;
    private final Supplier<TransactionSynchronizationRegistry> transactionSynchronizationRegistrySupplier;

    public DataSourceService(final Consumer<AgroalDataSource> consumer,
                             final Supplier<Class> driverSupplier,
                             final Supplier<AuthenticationContext> authenticationContextSupplier,
                             final Supplier<ExceptionSupplier<CredentialSource, Exception>> credentialSourceSupplier,
                             final Supplier<TransactionSynchronizationRegistry> transactionSynchronizationRegistrySupplier,
                             String dataSourceName, String jndiName, boolean jta, boolean connectable, boolean xa, AgroalDataSourceConfigurationSupplier dataSourceConfiguration) {
        this.consumer = consumer;
        this.driverSupplier = driverSupplier;
        this.authenticationContextSupplier = authenticationContextSupplier;
        this.credentialSourceSupplier = credentialSourceSupplier;
        this.transactionSynchronizationRegistrySupplier = transactionSynchronizationRegistrySupplier;
        this.dataSourceName = dataSourceName;
        this.jndiName = jndiName;
        this.jta = jta;
        this.connectable = connectable;
        this.xa = xa;
        this.dataSourceConfiguration = dataSourceConfiguration;
    }

    @Override
    public void start(StartContext context) throws StartException {
        Class<?> providerClass = driverSupplier != null ? driverSupplier.get() : null;
        if (xa) {
            if (!XADataSource.class.isAssignableFrom(providerClass)) {
                throw AgroalLogger.SERVICE_LOGGER.invalidXAConnectionProvider();
            }
        } else {
            if (providerClass != null && !DataSource.class.isAssignableFrom(providerClass) && !Driver.class.isAssignableFrom(providerClass)) {
                throw AgroalLogger.SERVICE_LOGGER.invalidConnectionProvider();
            }
        }

        dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().connectionProviderClass(providerClass);

        if (jta || xa) {
            TransactionManager transactionManager = ContextTransactionManager.getInstance();
            TransactionSynchronizationRegistry transactionSynchronizationRegistry = transactionSynchronizationRegistrySupplier != null ? transactionSynchronizationRegistrySupplier.get() : null;

            if (transactionManager == null || transactionSynchronizationRegistry == null) {
                throw AgroalLogger.SERVICE_LOGGER.missingTransactionManager();
            }
            TransactionIntegration txIntegration = new NarayanaTransactionIntegration(transactionManager, transactionSynchronizationRegistry, jndiName, connectable);
            dataSourceConfiguration.connectionPoolConfiguration().transactionIntegration(txIntegration);
        }

        AuthenticationContext authenticationContext = authenticationContextSupplier != null ? authenticationContextSupplier.get() : null;

        if (authenticationContext != null) {
            try {
                // Probably some other thing should be used as URI. Using jndiName for consistency with the datasources subsystem (simplicity as a bonus)
                URI targetURI = new URI(jndiName);

                NameCallback nameCallback = new NameCallback("Username: ");
                PasswordCallback passwordCallback = new PasswordCallback("Password: ", false);
                CredentialCallback credentialCallback = new CredentialCallback(GSSKerberosCredential.class);

                AuthenticationConfiguration authenticationConfiguration = AUTH_CONFIG_CLIENT.getAuthenticationConfiguration(targetURI, authenticationContext, -1, "jdbc", "jboss");
                AUTH_CONFIG_CLIENT.getCallbackHandler(authenticationConfiguration).handle(new Callback[] { nameCallback , passwordCallback, credentialCallback});

                // if a GSSKerberosCredential was found, add the enclosed GSSCredential and KerberosTicket to the private set in the Subject.
                if (credentialCallback.getCredential() != null) {
                    GSSKerberosCredential kerberosCredential = credentialCallback.getCredential(GSSKerberosCredential.class);

                    // use the GSSName to build a kerberos principal
                    dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().principal(new NamePrincipal(kerberosCredential.getGssCredential().getName().toString()));

                    dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().credential(kerberosCredential.getKerberosTicket());
                    dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().credential(kerberosCredential.getGssCredential());
                }

                // use the name / password from the callbacks
                if (nameCallback.getName() != null) {
                    dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().principal(new NamePrincipal(nameCallback.getName()));
                }
                if (passwordCallback.getPassword() != null) {
                    dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().credential(new SimplePassword(new String(passwordCallback.getPassword())));
                }

            } catch (URISyntaxException | UnsupportedCallbackException | IOException | GSSException e) {
                throw AgroalLogger.SERVICE_LOGGER.invalidAuthentication(e, dataSourceName);
            }
        }

        ExceptionSupplier<CredentialSource, Exception> credentialSourceExceptionExceptionSupplier = credentialSourceSupplier != null ? credentialSourceSupplier.get() : null;

        if (credentialSourceExceptionExceptionSupplier != null) {
            try {
                String password = new String(credentialSourceExceptionExceptionSupplier.get().getCredential(PasswordCredential.class).getPassword(ClearPassword.class).getPassword());
                dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().credential(new SimplePassword(password));
            } catch (Exception e) {
                throw AgroalLogger.SERVICE_LOGGER.invalidCredentialSourceSupplier(e, dataSourceName);
            }
        }

        try {
            agroalDataSource = AgroalDataSource.from(dataSourceConfiguration, new LoggingDataSourceListener(dataSourceName));

            ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
            BinderService binderService = new BinderService(bindInfo.getBindName());
            binderService.getManagedObjectInjector().inject(new ImmediateManagedReferenceFactory(agroalDataSource));
            context.getChildTarget().addService(bindInfo.getBinderServiceName(), binderService)
                   .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                   .install();

            if (xa) {
                AgroalLogger.SERVICE_LOGGER.startedXADataSource(dataSourceName, jndiName);
            } else {
                AgroalLogger.SERVICE_LOGGER.startedDataSource(dataSourceName, jndiName);
            }
        } catch (SQLException e) {
            agroalDataSource = null;
            if (xa) {
                throw AgroalLogger.SERVICE_LOGGER.xaDatasourceStartException(e, dataSourceName);
            } else {
                throw AgroalLogger.SERVICE_LOGGER.datasourceStartException(e, dataSourceName);
            }
        }
        consumer.accept(agroalDataSource);
    }

    @Override
    public void stop(StopContext context) {
        consumer.accept(null);
        agroalDataSource.close();
        if (xa) {
            AgroalLogger.SERVICE_LOGGER.stoppedXADataSource(dataSourceName);
        } else {
            AgroalLogger.SERVICE_LOGGER.stoppedDataSource(dataSourceName);
        }
    }

    @Override
    public AgroalDataSource getValue() throws IllegalStateException, IllegalArgumentException {
        return agroalDataSource;
    }

    @Override
    public AgroalDataSource get() {
        return agroalDataSource;
    }

}

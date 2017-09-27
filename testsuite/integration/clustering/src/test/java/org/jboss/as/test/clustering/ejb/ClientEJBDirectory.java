package org.jboss.as.test.clustering.ejb;

import javax.ejb.EJBHome;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

import org.jboss.ejb.client.ClusterAffinity;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBHomeLocator;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.wildfly.transaction.client.RemoteTransactionContext;

public class ClientEJBDirectory implements EJBDirectory {

    private final String appName;
    private final String moduleName;

    public ClientEJBDirectory(String moduleName) {
        this("", moduleName);
    }

    public ClientEJBDirectory(String appName, String moduleName) {
        this.appName = appName;
        this.moduleName = moduleName;
    }

    @Override
    public <T> T lookupStateful(String beanName, Class<T> beanInterface) throws NamingException {
        try {
            return createProxy(EJBClient.createSession(this.createStatelessLocator(beanName, beanInterface)));
        } catch (Exception e) {
            NamingException exception = new NamingException();
            exception.initCause(e);
            throw exception;
        }
    }

    @Override
    public <T> T lookupStateless(String beanName, Class<T> beanInterface) throws NamingException {
        return createProxy(this.createStatelessLocator(beanName, beanInterface));
    }

    @Override
    public <T> T lookupSingleton(String beanName, Class<T> beanInterface) throws NamingException {
        return createProxy(this.createStatelessLocator(beanName, beanInterface));
    }

    @Override
    public <T extends EJBHome> T lookupHome(String beanName, Class<T> homeInterface) throws NamingException {
        return createProxy(new EJBHomeLocator<>(homeInterface, this.appName, this.moduleName, beanName));
    }

    private <T> StatelessEJBLocator<T> createStatelessLocator(String beanName, Class<T> beanInterface) {
        return new StatelessEJBLocator<>(beanInterface, this.appName, this.moduleName, beanName);
    }

    private static <T> T createProxy(EJBLocator<T> locator) {
        T bean = EJBClient.createProxy(locator);
        EJBClient.setStrongAffinity(bean, new ClusterAffinity("ejb"));
        return bean;
    }

    @Override
    public UserTransaction lookupUserTransaction() throws NamingException {
        return RemoteTransactionContext.getInstance().getUserTransaction();
    }

    @Override
    public void close() throws NamingException {
    }
}

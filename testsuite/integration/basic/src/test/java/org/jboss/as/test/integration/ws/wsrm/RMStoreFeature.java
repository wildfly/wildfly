package org.jboss.as.test.integration.ws.wsrm;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.ws.rm.RM11Constants;
import org.apache.cxf.ws.rm.feature.RMFeature;
import org.apache.cxf.ws.rm.persistence.jdbc.RMTxStore;
import org.jboss.wsf.spi.WSFException;

/**
 * @author Tomas Hofman (thofman@redhat.com)
 */
public class RMStoreFeature extends RMFeature {

    private static final Logger LOGGER = Logger.getLogger(RMStoreFeature.class.getName());

    public static final String dataSourceName = "java:jboss/datasources/ExampleDS";
    private InitialContext ctx;

    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        RMTxStore rmStore = new RMTxStore();
        DataSource dataSource;
        try {
            if (ctx == null) {
                ctx = new InitialContext();
            }
            dataSource = (DataSource) ctx.lookup(dataSourceName);
            rmStore.setDataSource(dataSource);
        } catch (NamingException e) {
            LOGGER.log(Level.SEVERE, "Can't create datasource with " + dataSourceName, e);
            throw new WSFException(e);
        }

        rmStore.init();
        this.setStore(rmStore);
        this.setRMNamespace(RM11Constants.NAMESPACE_URI);

        super.initializeProvider(provider, bus);
    }

}

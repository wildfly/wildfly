package org.jboss.as.test.integration.jca.classloading;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup(DataSourceTcclDatasourceClassTestCase.Setup.class)
public class DataSourceTcclDatasourceClassTestCase extends AbstractDataSourceClassloadingTestCase {

    public static class Setup extends AbstractDataSourceClassloadingTestCase.Setup {

        public Setup() {
            super("driver-datasource-class-name", "org.jboss.as.test.integration.jca.classloading.ClassloadingDataSource");
        }
    }
}

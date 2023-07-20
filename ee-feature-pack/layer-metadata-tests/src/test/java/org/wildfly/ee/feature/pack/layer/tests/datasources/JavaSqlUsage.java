package org.wildfly.ee.feature.pack.layer.tests.datasources;

import java.sql.Connection;

public class JavaSqlUsage {
    Connection connection;
    // Usage from the method doesn't trigger the rule. It seems to need to be a field
    //public void useConnection() {
    //    Connection connection = null;
    //}
}

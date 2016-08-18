package org.jboss.as.test.integration.jca.metrics;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import org.jboss.as.connector.subsystems.datasources.DataSourcesExtension;
import org.jboss.as.connector.subsystems.datasources.Namespace;
import org.jboss.as.test.integration.management.jca.DsMgmtTestBase;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

/**
 * @author Stuart Douglas
 */
public abstract class JCAMetrictsTestBase extends DsMgmtTestBase {


    //@Before - called from each test
    /*
     * Load data source model, stored in specified file to the configuration
     */
    protected void setModel(String filename) throws Exception {
        String xml = FileUtils.readFile(JCAMetrictsTestBase.class, "data-sources/" + filename);
        List<ModelNode> operations = xmlToModelOperations(xml, Namespace.CURRENT.getUriString(), new DataSourcesExtension.DataSourceSubsystemParser());
        executeOperation(operationListToCompositeOperation(operations));
    }

    /*

     * Bad model must throw an Exception during setModel methos call. To work around wrong test case
     * removeDs() method is added.
     */
    protected void setBadModel(String filename) throws Exception {
        setModel(filename);
        removeDs();
    }


    protected void testStatistics(String configFile) throws Exception {

        setModel(configFile);

        try {
            final ModelNode poolAddress = new ModelNode().set(baseAddress);
            poolAddress.add("statistics", "pool");

            ModelNode operation = new ModelNode();
            operation.get(OP).set("read-resource");
            operation.get(OP_ADDR).set(poolAddress);
            operation.get(INCLUDE_RUNTIME).set(true);
            ModelNode result = executeOperation(operation);
            Assert.assertTrue("ActiveCount", result.hasDefined("ActiveCount"));

            final ModelNode jdbcAddress = new ModelNode().set(baseAddress);
            jdbcAddress.add("statistics", "jdbc");

            operation.get(OP_ADDR).set(jdbcAddress);
            result = executeOperation(operation);
            Assert.assertTrue("PreparedStatementCacheAccessCount", result.hasDefined("PreparedStatementCacheAccessCount"));
        } finally {
            removeDs();
        }
    }
}

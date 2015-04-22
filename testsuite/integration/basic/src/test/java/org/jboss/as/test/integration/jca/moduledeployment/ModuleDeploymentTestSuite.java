package org.jboss.as.test.integration.jca.moduledeployment;

/**
 * @author Tomaz Cerar (c) 2015 Red Hat Inc.
 */

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        BasicFlatTestCase.class,
        BasicJarTestCase.class,
        InflowFlatTestCase.class,
        InflowJarTestCase.class,
        MultiActivationFlatTestCase.class,
        MultiActivationJarTestCase.class,
        MultiObjectActivationFlatTestCase.class,
        MultiObjectActivationJarTestCase.class,
        PartialObjectActivationFlatTestCase.class,
        PartialObjectActivationJarTestCase.class,
        PureFlatTestCase.class,
        PureJarTestCase.class,
        TwoModulesFlatTestCase.class,
        TwoModulesJarTestCase.class,
        TwoModulesOfDifferentTypeTestCase.class,
        TwoRaFlatTestCase.class,
        TwoRaJarTestCase.class

})
public class ModuleDeploymentTestSuite {
}

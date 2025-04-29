package org.wildfly.clustering.ejb.client;

import org.jboss.ejb.client.EJBModuleIdentifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

import java.io.IOException;

/**
 * Unit test for {@link EJBModuleIdentifierMarshaller}.
 * @author Richard Achmatowicz
 */
public class EJBModuleIdentifierMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) throws IOException {
        Tester<EJBModuleIdentifier> tester = factory.createTester(EJBModuleIdentifierMarshallerTestCase::assertEquals);
        tester.accept(new EJBModuleIdentifier("app", "module", "distinct"));
        // TODO: testing for null fields
    }

    static void assertEquals(EJBModuleIdentifier moduleId1, EJBModuleIdentifier moduleId2) {
        Assertions.assertEquals(moduleId1.getAppName(), moduleId2.getAppName());
        Assertions.assertEquals(moduleId1.getModuleName(), moduleId2.getModuleName());
        Assertions.assertEquals(moduleId1.getDistinctName(), moduleId2.getDistinctName());
    }

}

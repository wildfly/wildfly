/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.beanvalidation.hibernate.scriptassert;

import java.sql.SQLException;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * AS7-1110
 * <p/>
 * Tests that hibernate validator @ScriptAssert works correctly
 * <p/>
 * This is a non-standard extension provided by hibernate validator
 *
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
public class ScriptAssertTestCase {

    private static final Logger logger = Logger.getLogger(ScriptAssertTestCase.class);

    @BeforeAll
    public static void beforeClass() {
        // https://issues.redhat.com/browse/WFLY-15719
        AssumeTestGroupUtil.assumeJDKVersionBefore(15);
    }

    @BeforeEach
    public void runOnlyAgainstSunJDK() {
        // Since this test apparently fails (https://issues.jboss.org/browse/AS7-2166) against OpenJDK, due to missing Javascript engine, let's just enable this test against Sun/Oracle JDK (which is known/expected to pass).
        // If an assumption fails, then the @Test(s) are skipped
        final boolean compatibleJRE = isCompatibleJRE();
        logger.trace("Current JRE is " + (compatibleJRE ? "compatible, running " : "incompatible, skipping ") + " tests in " + ScriptAssertTestCase.class.getSimpleName());
        Assumptions.assumeTrue(compatibleJRE);
    }

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "testscriptassert.war");
        war.addPackage(ScriptAssertTestCase.class.getPackage());
        return war;

    }


    @Test
    public void testScriptAssert() throws NamingException, SQLException {
        Validator validator = (Validator) new InitialContext().lookup("java:comp/Validator");
        final Set<ConstraintViolation<ScriptAssertBean>> result = validator.validate(new ScriptAssertBean());
        Assertions.assertEquals(1, result.size());
    }

    /**
     * Returns true if this testcase is expected to pass against the current Java runtime. Else returns false.
     *
     * @return
     */
    private boolean isCompatibleJRE() {
        final String javaRuntimeName = System.getProperty("java.runtime.name");
        // The OpenJDK runtime has a value of "OpenJDK Runtime Environment" for the java.runtime.name system property. Since this test isn't passing against OpenJDK, we classify it as incompatible and return false
        // if OpenJDK runtime is identified.
        // Note, if this test is failing against other JREs (like IBM), add another check with the appropriate value for IBM here.
        return !javaRuntimeName.contains("OpenJDK");
    }
}

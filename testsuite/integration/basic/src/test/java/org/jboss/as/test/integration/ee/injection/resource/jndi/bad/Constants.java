/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.jndi.bad;

/**
 * @author baranowb
 */
public interface Constants {

    String TEST_MODULE_NAME = "BadDonkeyModule";
    String TEST_MODULE_NAME_FULL = "test." + TEST_MODULE_NAME;

    String TESTED_DU_NAME = "BadTest";
    String TESTED_ARCHIVE_NAME = TESTED_DU_NAME + ".jar";


    String JNDI_NAME_GLOBAL = "java:global/" + TESTED_DU_NAME + "/ResourceEJBImpl";
    String JNDI_NAME_BAD = "java:jboss:/" + TESTED_DU_NAME + "/ResourceEJBImpl";

    String ERROR_MESSAGE = "WFLYNAM0033";
}

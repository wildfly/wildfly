/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.hibernate.search;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * JipiJapa message range is 20200-20299
 * note: keep duplicate messages in sync between different sub-projects that use the same messages
 */
@MessageLogger(projectCode = "JIPISEARCH")
public interface JpaHibernateSearchLogger extends BasicLogger {

    /**
     * A logger with the category {@code org.jipijapa}.
     */
    JpaHibernateSearchLogger JPA_HIBERNATE_SEARCH_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), JpaHibernateSearchLogger.class, "org.jipijapa");

    @Message(id = 20290, value = "Failed to parse property '%2$s' while integrating Hibernate Search into persistence unit '%1$s")
    IllegalStateException failOnPropertyParsingForIntegration(String puUnitName, String propertyKey, @Cause Exception cause);

}

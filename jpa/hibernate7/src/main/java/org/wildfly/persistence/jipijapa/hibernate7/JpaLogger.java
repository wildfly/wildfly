/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.persistence.jipijapa.hibernate7;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.lang.invoke.MethodHandles;

import org.hibernate.boot.archive.spi.ArchiveException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * JipiJapa message range is 20200-20299
 * note: keep duplicate messages in sync between different sub-projects that use the same messages
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Scott Marlow
 */
@MessageLogger(projectCode = "JIPIORMV7")
public interface JpaLogger extends BasicLogger {

    /**
     * A logger with the category {@code org.jipijapa}.
     */
    JpaLogger JPA_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), JpaLogger.class, "org.jipijapa");

    /**
     * Inform that the Hibernate second level cache is enabled.
     *
     * @param puUnitName the persistence unit name
     */
    @LogMessage(level = INFO)
    @Message(id = 20260, value = "Second level cache enabled for %s")
    void secondLevelCacheIsEnabled(Object puUnitName);

    /**
     * Creates an exception indicating that Hibernate ORM did not register the expected LifeCycleListener
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 20261, value = "Hibernate ORM did not register LifeCycleListener")
    IllegalStateException HibernateORMDidNotRegisterLifeCycleListener();

    @LogMessage(level = WARN)
    @Message(id = 20262, value = "Application custom cache region setting is ignored %s=%s")
    void ignoredCacheRegionSetting(String propertyName, String setting );

    /**
     * Creates an exception indicating application is setting persistence unit property "hibernate.id.new_generator_mappings" to
     * false which indicates that the old ID generator should be used, however Hibernate ORM 6 does not include the old ID generator.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 20263, value = "hibernate.id.new_generator_mappings set to false is not supported" +
            ", remove the setting or set to true.  "+
            "Refer to Hibernate ORM migration documentation for how to update the next id state in the application database.")
    IllegalStateException failOnIncompatibleSetting();

    @Message(id = 20264, value = "Unable to open VirtualFile-based InputStream")
    ArchiveException unableOpenInputStream(@Cause Throwable cause);

}

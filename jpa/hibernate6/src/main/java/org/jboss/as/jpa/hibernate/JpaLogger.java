/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.jpa.hibernate;

import static org.jboss.logging.Logger.Level.INFO;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * JipiJapa message range is 20200-20299
 * note: keep duplicate messages in sync between different sub-projects that use the same messages
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Scott Marlow
 */
@MessageLogger(projectCode = "JIPIORMV6")
public interface JpaLogger extends BasicLogger {

    /**
     * A logger with the category {@code org.jipijapa}.
     */
    JpaLogger JPA_LOGGER = Logger.getMessageLogger(JpaLogger.class, "org.jipijapa");

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

}

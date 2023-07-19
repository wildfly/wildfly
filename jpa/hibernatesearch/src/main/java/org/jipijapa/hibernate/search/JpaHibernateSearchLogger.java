/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jipijapa.hibernate.search;

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
    JpaHibernateSearchLogger JPA_HIBERNATE_SEARCH_LOGGER = Logger.getMessageLogger(JpaHibernateSearchLogger.class, "org.jipijapa");

    @Message(id = 20290, value = "Failed to parse property '%2$s' while integrating Hibernate Search into persistence unit '%1$s")
    IllegalStateException failOnPropertyParsingForIntegration(String puUnitName, String propertyKey, @Cause Exception cause);

}

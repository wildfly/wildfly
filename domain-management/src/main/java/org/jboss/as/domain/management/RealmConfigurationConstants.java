/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management;

/**
 * Constants used by the security realms to report the details of the configuration for passing to the transport specific
 * configuration.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface RealmConfigurationConstants {

    String DIGEST_PLAIN_TEXT = "org.jboss.as.domain.management.digest.plain_text";
    String LOCAL_DEFAULT_USER = "org.jboss.as.domain.management.local.default_user";
    String SUBJECT_CALLBACK_SUPPORTED = "org.jboss.as.domain.management.subject_callback_supported";
    String VERIFY_PASSWORD_CALLBACK_SUPPORTED = "org.jboss.as.domain.management.verify_password_callback_supported";

}

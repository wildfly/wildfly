/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jaxrs;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface JaxrsLogger extends BasicLogger {

    /**
     * A logger with the category {@code org.jboss.jaxrs}.
     */
    JaxrsLogger JAXRS_LOGGER = Logger.getMessageLogger(JaxrsLogger.class, "org.jboss.jaxrs");

    /**
     * Logs a warning message indicating the annotation, represented by the {@code annotation} parameter, not on Class,
     * represented by the {@code target} parameter.
     *
     * @param annotation the missing annotation.
     * @param target     the target.
     */
    @LogMessage(level = WARN)
    @Message(id = 11200, value = "%s annotation not on Class: %s")
    void classAnnotationNotFound(String annotation, AnnotationTarget target);

    /**
     * Logs a warning message indicating the annotation, represented by the {@code annotation} parameter, not on Class
     * or Method, represented by the {@code target} parameter.
     *
     * @param annotation the missing annotation.
     * @param target     the target.
     */
    @LogMessage(level = WARN)
    @Message(id = 11201, value = "%s annotation not on Class or Method: %s")
    void classOrMethodAnnotationNotFound(String annotation, AnnotationTarget target);

    /**
     * Logs an error message indicating more than one mapping found for JAX-RS servlet, represented by the
     * {@code servletName} parameter, the second mapping, represented by the {@code pattern} parameter, will not work.
     *
     * @param servletName the name of the servlet.
     * @param pattern     the pattern.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11202, value = "More than one mapping found for JAX-RS servlet: %s the second mapping %s will not work")
    void moreThanOneServletMapping(String servletName, String pattern);

    /**
     * Logs a warning message indicating no servlet mappings found for the JAX-RS application, represented by the
     * {@code servletName} parameter, either annotate with {@link javax.ws.rs.ApplicationPath @ApplicationPath} or add
     * a {@code servlet-mapping} in the web.xml.
     *
     * @param servletName the servlet name.
     */
    @LogMessage(level = WARN)
    @Message(id = 11203, value = "No Servlet mappings found for JAX-RS application: %s either annotate it with @ApplicationPath or add a servlet-mapping in web.xml")
    void noServletMappingFound(String servletName);

    /**
     * Logs a warning message indicating that {@code resteasy.scan} was found in the {@code web.xml} and is not
     * necessary.
     */
    @LogMessage(level = WARN)
    @Message(id = 11204, value = "%s found and ignored in web.xml. This is not necessary, as Resteasy will use the container integration in the JAX-RS 1.1 specification in section 2.3.2")
    void resteasyScanWarning(String param);
}

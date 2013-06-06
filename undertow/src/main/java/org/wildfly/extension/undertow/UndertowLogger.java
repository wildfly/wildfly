/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import java.net.InetSocketAddress;

import io.undertow.server.HttpHandler;

import org.jboss.jandex.ClassInfo;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * <p/>
 * This file is using the subset 17500-17699 for logger messages.
 * <p/>
 * See <a href="http://community.jboss.org/docs/DOC-16810">http://community.jboss.org/docs/DOC-16810</a> for the full
 * list of currently reserved JBAS message id blocks.
 * <p/>
 * This logger also reuses some messages from WebLogger in range 18200-18300
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface UndertowLogger extends BasicLogger {

    /**
     * A root logger with the category of the package name.
     */
    UndertowLogger ROOT_LOGGER = Logger.getMessageLogger(UndertowLogger.class, UndertowLogger.class.getPackage().getName());

    ////////////////////////////////////////////////////////////
    //18200-18226 are copied across from the old web subsystem

    @LogMessage(level = ERROR)
    @Message(id = 18200, value = "Failed to start welcome context")
    void stopWelcomeContextFailed(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 18201, value = "Failed to destroy welcome context")
    void destroyWelcomeContextFailed(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 18202, value = "Error calling onStartup for servlet container initializer: %s")
    void sciOnStartupError(String sciClassName, @Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 18203, value = "Error instantiating container component: %s")
    void componentInstanceCreationFailed(String className, @Cause Throwable cause);

    @LogMessage(level = WARN)
    @Message(id = 18204, value = "Clustering not supported, falling back to non-clustered session manager")
    void clusteringNotSupported();

    @LogMessage(level = ERROR)
    @Message(id = 18205, value = "Cannot setup overlays for [%s] due to custom resources")
    void noOverlay(String webappPath);

    @LogMessage(level = ERROR)
    @Message(id = 18206, value = "Webapp [%s] is unavailable due to startup errors")
    void unavailable(String webappPath);

    @LogMessage(level = ERROR)
    @Message(id = 18208, value = "Failed to start context")
    void stopContextFailed(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 18209, value = "Failed to destroy context")
    void destroyContextFailed(@Cause Throwable cause);

    @LogMessage(level = INFO)
    @Message(id = 18210, value = "Register web context: %s")
    void registerWebapp(String webappPath);

    @LogMessage(level = ERROR)
    @Message(id = 18214, value = "Error during login/password authenticate")
    void authenticateError(@Cause Throwable t);

    @LogMessage(level = ERROR)
    @Message(id = 18215, value = "Error during certificate authenticate")
    void authenticateErrorCert(@Cause Throwable t);

    @LogMessage(level = ERROR)
    @Message(id = 18216, value = "Error during digest authenticate")
    void authenticateErrorDigest(@Cause Throwable t);

    @LogMessage(level = ERROR)
    @Message(id = 18217, value = "Error obtaining authorization helper")
    void noAuthorizationHelper(@Cause Throwable t);

    @LogMessage(level = ERROR)
    @Message(id = 18218, value = "Exception in obtaining server authentication manager")
    void noServerAuthenticationManager(@Cause Throwable t);

    @LogMessage(level = ERROR)
    @Message(id = 18219, value = "JASPI validation for unprotected request context %s failed")
    void failJASPIValidation(String path);

    @LogMessage(level = ERROR)
    @Message(id = 18220, value = "Caught Exception: %s")
    void unsupportedEncoding(String encoding);

    @LogMessage(level = WARN)
    @Message(id = 18221, value = "Error forwarding to login page: %s")
    void errorForwardingToLoginPage(String encoding);

    @LogMessage(level = WARN)
    @Message(id = 18222, value = "Error forwarding to error page: %s")
    void errorForwardingToErrorPage(String encoding);

    @LogMessage(level = WARN)
    @Message(id = 18223, value = "Snapshot mode set to 'interval' but snapshotInterval is < 1 or was not specified, using 'instant'")
    void invalidSnapshotInterval();

    @LogMessage(level = INFO)
    @Message(id = 18224, value = "Unregister web context: %s")
    void unregisterWebapp(String webappPath);

    @LogMessage(level = INFO)
    @Message(id = 18226, value = "Skipped SCI for jar: %s.")
    void skippedSCI(String jar, @Cause Exception e);

    /*
    UNDERTOW messages start
     */

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 17500, value = "Could not initialize JSP")
    void couldNotInitJsp(@Cause ClassNotFoundException e);

    @LogMessage(level = ERROR)
    @Message(id = 17501, value = "Failed to purge EL cache.")
    void couldNotPurgeELCache(@Cause Exception exception);
    // id = 11500, value = "%s caught exception attempting to revert operation %s at address %s" -- now unused

    @LogMessage(level = INFO)
    @Message(id = 17502, value = "Undertow %s starting")
    void serverStarting(String version);

    @LogMessage(level = INFO)
    @Message(id = 17506, value = "Undertow %s stopping")
    void serverStopping(String version);

    /**
     * Creates an exception indicating the class, represented by the {@code className} parameter, cannot be accessed.
     *
     * @param name    name of the listener
     * @param address socket address
     */
    @LogMessage(level = INFO)
    @Message(id = 17519, value = "Undertow %s listener %s listening on %s")
    void listenerStarted(String type, String name, InetSocketAddress address);

    @LogMessage(level = INFO)
    @Message(id = 17520, value = "Undertow %s listener %s stopped, was bound to %s")
    void listenerStopped(String type, String name, InetSocketAddress address);

    @LogMessage(level = INFO)
    @Message(id = 17521, value = "Undertow %s listener %s suspending")
    void listenerSuspend(String type, String name);

    @LogMessage(level = INFO)
    @Message(id = 17522, value = "Could not load class designated by HandlesTypes [%s].")
    void cannotLoadDesignatedHandleTypes(ClassInfo classInfo, @Cause Exception e);

    @LogMessage(level = WARN)
    @Message(id = 17523, value = "Could not load web socket endpoint %s.")
    void couldNotLoadWebSocketEndpoint(String s, @Cause Exception e);

    @LogMessage(level = WARN)
    @Message(id = 17524, value = "Could not load web socket application config %s.")
    void couldNotLoadWebSocketConfig(String s, @Cause Exception e);

    @LogMessage(level = INFO)
    @Message(id = 17525, value = "Started http handler %s.")
    void startedHttpHandler(HttpHandler handler);

    @LogMessage(level = WARN)
    @Message(id = 17526, value = "Could not create redirect URI.")
    void invalidRedirectURI(@Cause Throwable cause);

    @LogMessage(level = INFO)
    @Message(id = 17527, value = "Creating file handler for path %s")
    void creatingFileHandler(String path);

    @LogMessage(level = INFO)
    @Message(id = 17528, value="registering handler %s under path '%s'")
    void registeringHandler(HttpHandler value, String locationPath);

    @LogMessage(level = WARN)
    @Message(id = 17529, value = "Could not resolve name in absolute ordering: %s")
    void invalidAbsoluteOrdering(String name);
}

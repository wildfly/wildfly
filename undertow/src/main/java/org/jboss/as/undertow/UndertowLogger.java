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

package org.jboss.as.undertow;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.net.InetSocketAddress;

import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.as.undertow.session.ClusteredSession;
import org.jboss.jandex.ClassInfo;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

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
    UndertowLogger WEB_SESSION_LOGGER = Logger.getMessageLogger(UndertowLogger.class, UndertowLogger.class.getPackage().getName() + ".sessions");

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

    @LogMessage(level = ERROR)
    @Message(id = 17507, value = "Failed to queue session replication for session %s")
    void failedQueueingSessionReplication(ClusteredSession<? extends OutgoingDistributableSessionData> session, @Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id = 17508, value = "Exception processing sessions")
    void exceptionProcessingSessions(@Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id = 17509, value = "Failed to store session %s")
    void failedToStoreSession(String realId, @Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id = 17510, value = "Failed to replicate session %s")
    void failedToReplicateSession(String idInternal, @Cause Exception e);

    @LogMessage(level = WARN)
    @Message(id = 17511, value = "Failed to passivate session %s")
    void errorPassivatingSession(String idInternal, @Cause Throwable t);

    @LogMessage(level = WARN)
    @Message(id = 17512, value = "Received notification for inactive session %s")
    void notificationForInactiveSession(String realId);

    @LogMessage(level = ERROR)
    @Message(id = 17513, value = "Failed to load passivated session %s")
    void failToPassivateLoad(String realId, @Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id = 17514, value = "Brute force cleanup failed for session %s")
    void failToBruteForceCleanup(String realId, @Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id = 17515, value = "Problem running expiration passivation")
    void processExpirationPassivationException(@Cause Exception ex);

    @LogMessage(level = ERROR)
    @Message(id = 17516, value = "Failed to passivate %s %s")
    void failToPassivate(String s, String realId, @Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id = 17517, value = "Failed to rollback transaction")
    void exceptionRollingBackTransaction(@Cause RuntimeException exception);

    @LogMessage(level = WARN)
    @Message(id = 17518, value = "Performing brute force cleanup on %s due to %s")
    void bruteForceCleanup(String realId, String localizedMessage);

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

}

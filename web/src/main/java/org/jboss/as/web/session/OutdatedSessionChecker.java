/**
 *
 */
package org.jboss.as.web.session;

import org.jboss.as.clustering.web.OutgoingDistributableSessionData;

/**
 *
 *
 * @author Brian Stansberry
 *
 * @version $Revision: $
 */
public interface OutdatedSessionChecker {
    boolean isSessionOutdated(ClusteredSession<? extends OutgoingDistributableSessionData> session);
}

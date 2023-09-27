/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws.serviceref;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public interface StatelessRemote {
    String echo1(String string) throws Exception;

    String echo2(String string) throws Exception;

    String echo3(String string) throws Exception;

    String echo4(String string) throws Exception;

    String echoCDI(String string) throws Exception;
}

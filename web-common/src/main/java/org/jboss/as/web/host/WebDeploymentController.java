/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.web.host;

/**
 * @author Stuart Douglas
 */
public interface WebDeploymentController {

    void create() throws Exception;

    void start() throws Exception;

    void stop() throws Exception;

    void destroy() throws Exception;

}

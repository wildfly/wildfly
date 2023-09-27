/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.suspend;

import jakarta.jws.WebService;
import java.util.List;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@WebService
public interface ExecutorService {

    void init(String activationServiceUrl, String remoteServiceUrl);

    void begin() throws Exception;

    void commit() throws Exception;

    void rollback() throws Exception;

    void enlistParticipant() throws Exception;

    void execute() throws Exception;

    void reset();

    List<String> getParticipantInvocations();

}

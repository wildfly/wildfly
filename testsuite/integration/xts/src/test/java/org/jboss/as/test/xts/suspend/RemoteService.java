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
public interface RemoteService {

    void execute() throws Exception;

    List<String> getParticipantInvocations();

    void reset();

}

package org.jboss.as.test.integration.logging.util;

import java.io.File;
import java.io.IOException;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;

public abstract class AbstractLoggingTest {
	public static File prepareLogFile(final ManagementClient client,
			final String filename) throws IOException, MgmtOperationException {
		File logFile;
		final ModelNode address = PathAddress.pathAddress(
				PathElement.pathElement(ModelDescriptionConstants.PATH,
						"jboss.server.log.dir")).toModelNode();
		final ModelNode op = Operations.createReadAttributeOperation(address,
				ModelDescriptionConstants.PATH);
		final ModelNode result = client.getControllerClient().execute(op);
		if (Operations.isSuccessfulOutcome(result)) {
			logFile = new File(Operations.readResult(result).asString(),
					filename);
			logFile.delete();
			return logFile;
		}
		throw new MgmtOperationException("Failed to read the path resource",
				op, result);
	}

	public static void testCLI() {

	}

}

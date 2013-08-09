package org.jboss.as.patching.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.DefaultCompleter.CandidatesProvider;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

class CandidatesProviders {

    private static Collection<String> getChildrenNames(ModelControllerClient client, ModelNode address, String childType) {
        if(client == null) {
            return Collections.emptyList();
        }

        final ModelNode request = new ModelNode();
        request.get(Util.ADDRESS).set(address);
        request.get(Util.OPERATION).set(Util.READ_CHILDREN_NAMES);
        request.get(Util.CHILD_TYPE).set(childType);

        final ModelNode response;
        try {
            response = client.execute(request);
        } catch (IOException e) {
            return Collections.emptyList();
        }
        final ModelNode result = response.get(Util.RESULT);
        if(!result.isDefined()) {
            return Collections.emptyList();
        }
        final List<ModelNode> list = result.asList();
        final List<String> names = new ArrayList<String>(list.size());
        for(ModelNode node : list) {
            names.add(node.asString());
        }
        return names;
    }

    static final CandidatesProvider HOSTS = new CandidatesProvider() {
        @Override
        public Collection<String> getAllCandidates(CommandContext ctx) {
            final ModelControllerClient client = ctx.getModelControllerClient();
            final ModelNode address = new ModelNode().setEmptyList();
            return getChildrenNames(client, address, Util.HOST);
        }
    };

    static final CandidatesProvider newServerCandidatesProvider(final ArgumentWithValue host) {
        return new CandidatesProvider() {
            @Override
            public Collection<String> getAllCandidates(CommandContext ctx) {
                final ModelControllerClient client = ctx.getModelControllerClient();
                final ModelNode address = new ModelNode().set(Util.HOST, host.getValue(ctx.getParsedCommandLine()));
                return getChildrenNames(client, address, Util.SERVER);
            }
        };
    }
}

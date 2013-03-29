package org.jboss.as.undertow;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import javax.xml.stream.XMLStreamException;

import io.undertow.server.HttpHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class HandlerFactory {
    private static final Map<String, Handler> handlerMap = new HashMap<>();
    private static final List<Handler> handlers = new LinkedList<>();

    static {
        loadRegisteredHandlers();
    }

    private static void loadRegisteredHandlers() {


        try {
            final Module module = Module.getBootModuleLoader().loadModule(ModuleIdentifier.create("org.jboss.as.undertow", "main"));
            if (module != null) {
                for (final Handler handler : module.loadService(Handler.class)) {
                    handlers.add(handler);
                    handlerMap.put(handler.getName(), handler);
                }
            }
        } catch (ModuleLoadException e) {
            //e.printStackTrace();
        }
        if (handlers.isEmpty()) {
            ServiceLoader<Handler> loader = ServiceLoader.load(Handler.class);
            for (final Handler handler : loader) {
                handlers.add(handler);
                handlerMap.put(handler.getName(), handler);
            }
        }
    }

    public static Map<String, Handler> getHandlerMap() {
        return handlerMap;
    }

    public static List<Handler> getHandlers() {
        return handlers;
    }

    public static void parseHandlers(XMLExtendedStreamReader reader, PathAddress parentAddress, List<ModelNode> list) throws XMLStreamException {
        Map<String, Handler> handlerMap = HandlerFactory.getHandlerMap();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String tagName = reader.getLocalName();
            Handler handler = handlerMap.get(tagName);
            if (handler != null) {
                handler.parse(reader, parentAddress, list);
            } else {
                throw UndertowMessages.MESSAGES.unknownHandler(tagName, reader.getLocation());
            }
        }
    }

    public static void persistHandlers(XMLExtendedStreamWriter writer, ModelNode model, boolean wrap) throws XMLStreamException {
        if (model.hasDefined(Constants.HANDLER)) {
            if (wrap) {
                writer.writeStartElement(Constants.HANDLERS);
            }
            Map<String, Handler> handlerMap = HandlerFactory.getHandlerMap();
            for (final Property handlerProp : model.get(Constants.HANDLER).asPropertyList()) {
                Handler handler = handlerMap.get(handlerProp.getName());
                handler.persist(writer, model);
            }
            if (wrap) {
                writer.writeEndElement();
            }
        }
    }

    public static HttpHandler getHandlerChain(final ModelNode model, final OperationContext context) throws OperationFailedException {
        HttpHandler last = null;
        for (Handler h : handlers) {
            ModelNode handlerModel = model.get(Constants.HANDLER, h.getName());
            if (handlerModel.isDefined()) {
                last = h.createHandler(last, context, handlerModel);
            }
        }
        return last;
    }
}

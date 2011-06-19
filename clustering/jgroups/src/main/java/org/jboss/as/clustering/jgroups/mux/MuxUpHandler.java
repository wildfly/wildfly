package org.jboss.as.clustering.jgroups.mux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jgroups.Event;
import org.jgroups.UpHandler;
import org.jgroups.blocks.mux.MuxHeader;
import org.jgroups.stack.StateTransferInfo;
import org.jgroups.util.ImmutableReference;

/**
 * Overrides superclass to allow state transfer multiplexing.
 *
 * @author Brian Stansberry
 */
public class MuxUpHandler extends org.jgroups.blocks.mux.MuxUpHandler {
    private final Map<Short, UpHandler> stateTransferHandlers = new ConcurrentHashMap<Short, UpHandler>();

    /**
     * Creates a multiplexing up handler, with no default handler.
     */
    public MuxUpHandler() {
        super();
    }

    /**
     * Creates a multiplexing up handler using the specified default handler.
     *
     * @param defaultHandler a default up handler to handle messages with no {@link MuxHeader}
     */
    public MuxUpHandler(UpHandler defaultHandler) {
        super(defaultHandler);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jgroups.blocks.mux.Muxer#add(short, java.lang.Object)
     */
    @Override
    public void add(short id, UpHandler handler) {
        super.add(id, handler);
        if (handler instanceof StateTransferFilter) {
            stateTransferHandlers.put(Short.valueOf(id), handler);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jgroups.blocks.mux.Muxer#remove(short)
     */
    @Override
    public void remove(short id) {
        super.remove(id);
        stateTransferHandlers.remove(Short.valueOf(id));
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jgroups.blocks.mux.MuxUpHandler#handleStateTransferEvent(org.jgroups.Event)
     */
    @Override
    protected ImmutableReference<Object> handleStateTransferEvent(Event evt) {
        StateTransferInfo info = (StateTransferInfo) evt.getArg();
        for (UpHandler uh : stateTransferHandlers.values()) {
            if (uh instanceof StateTransferFilter) {
                if (((StateTransferFilter) uh).accepts(info.state_id)) {
                    return new ImmutableReference<Object>(uh.up(evt));
                }
            }
        }

        return null;
    }
}

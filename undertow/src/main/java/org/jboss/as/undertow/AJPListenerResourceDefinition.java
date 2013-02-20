package org.jboss.as.undertow;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class AJPListenerResourceDefinition extends AbstractListenerResourceDefinition {
    protected static final AJPListenerResourceDefinition INSTANCE = new AJPListenerResourceDefinition();


    private AJPListenerResourceDefinition() {
        super(UndertowExtension.AJP_LISTENER_PATH);
    }

    @Override
    protected AbstractListenerAdd getAddHandler() {
        return new AJPListenerAdd(this);
    }
}

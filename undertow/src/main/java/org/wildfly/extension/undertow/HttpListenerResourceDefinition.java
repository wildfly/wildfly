package org.wildfly.extension.undertow;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class HttpListenerResourceDefinition extends AbstractListenerResourceDefinition {
    protected static final HttpListenerResourceDefinition INSTANCE = new HttpListenerResourceDefinition();


    private HttpListenerResourceDefinition() {
        super(UndertowExtension.HTTP_LISTENER_PATH);
    }

    @Override
    protected AbstractListenerAdd getAddHandler() {
        return new HttpListenerAdd(this);
    }
}

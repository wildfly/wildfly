package org.jboss.as.naming;

import javax.naming.Name;
import javax.naming.NamingException;

import org.jboss.as.naming.util.NameParser;
import org.jboss.msc.value.InjectedValue;

/**
 * Managed reference factory used for binding a context.
 *
 * @author Stuart Douglas
 */
public class ContextManagedReferenceFactory implements ManagedReferenceFactory {

    private final String name;
    private final InjectedValue<NamingStore> namingStoreInjectedValue = new InjectedValue<NamingStore>();

    public ContextManagedReferenceFactory(final String name) {
        this.name = name;
    }

    @Override
    public ManagedReference getReference() {
        final NamingStore namingStore = namingStoreInjectedValue.getValue();
        try {
            final Name name = NameParser.INSTANCE.parse(this.name);
            final NamingContext context =  new NamingContext(name, namingStore, null);
            return new ManagedReference() {
                @Override
                public void release() {

                }

                @Override
                public Object getInstance() {
                    return context;
                }
            };
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    public InjectedValue<NamingStore> getNamingStoreInjectedValue() {
        return namingStoreInjectedValue;
    }
}

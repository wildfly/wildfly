package org.jboss.as.domain.management.security;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

abstract class AbstractRealmPrincipal extends SecurityRealmPrincipal implements RealmPrincipal {

    private int hashBase = this.getClass().getName().hashCode();
    private final String realm;

    public AbstractRealmPrincipal(final String name) {
        super(name);
        this.realm = null;
    }

    public AbstractRealmPrincipal(final String realm, final String name) {
        super(name);
        if (name == null) {
            throw MESSAGES.canNotBeNull("realm");
        }
        this.realm = realm;
    }

    public String getRealm() {
        return realm;
    }

    public String getFullName() {
        return realm == null ? getName() : getName() + "@" + realm;
    }

    @Override
    public String toString() {
        return getFullName();
    }

    @Override
    public int hashCode() {
        return (super.hashCode() + hashBase) * (realm == null ? 101 : realm.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass()) ? equals((AbstractRealmPrincipal) obj) : false;

    }

    private boolean equals(AbstractRealmPrincipal user) {
        return this == user ? true : super.equals(user) && realm.equals(user.realm);
    }

}

attr = [enabled: 'false']

def security = {
    security(attr) {}
}
messaging.server.security.replaceNode({})
messaging.server.appendNode security
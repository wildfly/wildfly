attr = ['socket-binding': 'http']
attr['endpoint'] = 'http-acceptor'

def connector = {
    'http-connector'(attr) {}
}

messaging.server.appendNode connector
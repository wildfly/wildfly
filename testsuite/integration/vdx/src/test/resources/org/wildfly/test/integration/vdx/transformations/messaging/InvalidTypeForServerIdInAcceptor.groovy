attr = [name: 'in-vm']
attr['server-id'] = parameter

def inVmAcceptor = {
    'in-vm-acceptor'(attr) {}
}

messaging.server.'in-vm-acceptor'.replaceNode inVmAcceptor


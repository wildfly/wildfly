attr = [name: '#']
attr['dead-letter-address'] = 'jms.queue.DLQ'
attr['expiry-address'] = 'jms.queue.ExpiryQueue'
attr['max-size-bytes'] = parameter
attr['page-size-bytes'] = '2097152'
attr['message-counter-history-day-limit'] = '10'
attr['redistribution-delay'] = '1000'
attr['address-full-policy'] = 'PAGE'

def addressSettingNode = {
    'address-setting'(attr) {}
}

messaging.server.'address-setting'.replaceNode addressSettingNode

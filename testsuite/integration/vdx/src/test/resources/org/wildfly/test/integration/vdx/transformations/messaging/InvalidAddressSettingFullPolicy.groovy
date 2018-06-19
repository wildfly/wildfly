attr = [name: '#']
attr['dead-letter-address'] = 'jms.queue.DLQ'
attr['expiry-address'] = 'jms.queue.ExpiryQueue'
attr['max-size-bytes'] = '10485760'
attr['page-size-bytes'] = '2097152'
attr['message-counter-history-day-limit'] = '10'
attr['redistribution-delay'] = '1000'
// address-full-policy specifies what will happen if there is more than max-size-bytes in queue
// only DROP.FAIL,BLOCK and PAGE are allowed for this enum type
attr['address-full-policy'] = 'PAGES'

def addressSettingNode = {
    'address-setting'(attr) {}
}

messaging.server.'address-setting'.replaceNode addressSettingNode

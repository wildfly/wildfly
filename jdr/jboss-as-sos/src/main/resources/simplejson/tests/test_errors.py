from unittest import TestCase

import simplejson as json

class TestErrors(TestCase):
    def test_string_keys_error(self):
        data = [{'a': 'A', 'b': (2, 4), 'c': 3.0, ('d',): 'D tuple'}]
        self.assertRaises(TypeError, json.dumps, data)

    def test_decode_error(self):
        err = None
        try:
            json.loads('{}\na\nb')
        except json.JSONDecodeError, e:
            err = e
        else:
            self.fail('Expected JSONDecodeError')
        self.assertEquals(err.lineno, 2)
        self.assertEquals(err.colno, 1)
        self.assertEquals(err.endlineno, 3)
        self.assertEquals(err.endcolno, 2)

    def test_scan_error(self):
        err = None
        for t in (str, unicode):
            try:
                json.loads(t('{"asdf": "'))
            except json.JSONDecodeError, e:
                err = e
            else:
                self.fail('Expected JSONDecodeError')
            self.assertEquals(err.lineno, 1)
            self.assertEquals(err.colno, 9)
        
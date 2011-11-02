import unittest
import simplejson as json
from StringIO import StringIO

try:
    from collections import namedtuple
except ImportError:
    class Value(tuple):
        def __new__(cls, *args):
            return tuple.__new__(cls, args)

        def _asdict(self):
            return {'value': self[0]}
    class Point(tuple):
        def __new__(cls, *args):
            return tuple.__new__(cls, args)

        def _asdict(self):
            return {'x': self[0], 'y': self[1]}
else:
    Value = namedtuple('Value', ['value'])
    Point = namedtuple('Point', ['x', 'y'])

class TestNamedTuple(unittest.TestCase):
    def test_namedtuple_dumps(self):
        for v in [Value(1), Point(1, 2)]:
            d = v._asdict()
            l = list(v)
            self.assertEqual(d, json.loads(json.dumps(v)))
            self.assertEqual(
                d,
                json.loads(json.dumps(v, namedtuple_as_object=True)))
            self.assertEqual(d, json.loads(json.dumps(v, tuple_as_array=False)))
            self.assertEqual(
                d,
                json.loads(json.dumps(v, namedtuple_as_object=True,
                                      tuple_as_array=False)))
            self.assertEqual(
                l,
                json.loads(json.dumps(v, namedtuple_as_object=False)))
            self.assertRaises(TypeError, json.dumps, v,
                tuple_as_array=False, namedtuple_as_object=False)

    def test_namedtuple_dump(self):
        for v in [Value(1), Point(1, 2)]:
            d = v._asdict()
            l = list(v)
            sio = StringIO()
            json.dump(v, sio)
            self.assertEqual(d, json.loads(sio.getvalue()))
            sio = StringIO()
            json.dump(v, sio, namedtuple_as_object=True)
            self.assertEqual(
                d,
                json.loads(sio.getvalue()))
            sio = StringIO()
            json.dump(v, sio, tuple_as_array=False)
            self.assertEqual(d, json.loads(sio.getvalue()))
            sio = StringIO()
            json.dump(v, sio, namedtuple_as_object=True,
                      tuple_as_array=False)
            self.assertEqual(
                d,
                json.loads(sio.getvalue()))
            sio = StringIO()
            json.dump(v, sio, namedtuple_as_object=False)
            self.assertEqual(
                l,
                json.loads(sio.getvalue()))
            self.assertRaises(TypeError, json.dump, v, StringIO(),
                tuple_as_array=False, namedtuple_as_object=False)

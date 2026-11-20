"""Black-box checks of the Java CLI. Run from this directory."""
import subprocess
import unittest


class RouteTests(unittest.TestCase):
    def run_route(self, *args):
        return subprocess.run(["java", "RouteDesk.java", *args], capture_output=True, text=True)

    def test_itineraries(self):
        # Keep the endpoints the same so each case shows the effect of stairs or closures.
        for options, expected in [
            ([], "Library -> Commons -> Lab | 5 min"),
            (["--step-free"], "Library -> Garden -> Lab | 8 min"),
            (["--step-free", "--closed=Garden"], "Library -> Commons -> Studio -> Lab | 9 min"),
            (["--closed=Commons", "--closed=Garden"], "No route available"),
        ]:
            with self.subTest(options=options):
                result = self.run_route("Library", "Lab", *options)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertIn(expected, result.stdout)

    def test_same_stop(self):
        self.assertIn("Lab | 0 min", self.run_route("Lab", "Lab").stdout)

    def test_unknown_stop(self):
        result = self.run_route("Library", "Mars")
        self.assertEqual(result.returncode, 2)
        self.assertIn("Unknown stop", result.stderr)

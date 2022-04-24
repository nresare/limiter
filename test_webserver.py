#!/usr/bin/env python3
from http.server import HTTPServer, BaseHTTPRequestHandler


class SimpleHTTPRequestHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header("Content-type", "text/plain")
        self.end_headers()
        self.wfile.write(bytes(f"called with path {self.path}\n", "UTF-8"))


def main():
    port = 8081
    server = HTTPServer(('', port), SimpleHTTPRequestHandler)
    server.serve_forever()


if __name__ == "__main__":
    main()

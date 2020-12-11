# Copyright 2019, Oracle and/or its affiliates.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

import json
from http.server import BaseHTTPRequestHandler
from http.server import HTTPServer

class LogHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        print
        self.send_response(200)
        self.end_headers()
        if 'Content-Type' in self.headers and self.headers['Content-Type'] == 'application/json':
          if 'Content-Length' not in self.headers:
            print('Receiving a json request, but not an alert.')
            return
          length = int(self.headers['Content-Length'])
          data = json.loads(self.rfile.read(length).decode('utf-8'))
          for alert in data["alerts"]:
            print('!!! Receiving an alert.')
            print(json.dumps(alert, indent=2))
        else:
          print('Receiving a non-json post.')


PORT = 8080

if __name__ == '__main__':
   httpd = HTTPServer(('', PORT), LogHandler)
   print ('Webhook is serving at port', PORT)
   httpd.serve_forever()

import json
from http.server import BaseHTTPRequestHandler
from http.server import HTTPServer

class LogHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        print
        self.send_response(200)
        self.end_headers()
        if 'Content-Type' in self.headers and self.headers['Content-Type'] == 'application/json':
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

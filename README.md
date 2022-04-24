# Simple http rate limiter

This small projects implements a web server that proxies GET requests to a backend.
If two requests with the same normalised path arrive closer in time than MINIMAL_GAP
milliseconds from each other the proxy delays sending subsequent requests to the backend
such that there are at least MINIMAL_GAP milliseconds between the requests.

## Running and testing

All the settings are hard coded in `WebServer.java`. A tiny echoing web server is also
included in test_webserver.py. I have tested this with java 17.0.3 on macOS 12.3.1

Steps to test:
1. Build and run the unit tests with `./gradlew check`
2. start the echoing webserver (listening on port 8081) in one Terminal window
3. run the limiter web server with `./gradlew run` in another Terminal
4. send requests in a third terminal with `curl http://localhost:8080`
5. configure the MINIMAL_GAP with a request such as this one `curl http://localhost:8080/limit/4000`
   to set minimal gap to 4 seconds.
6. verify that with two subsequent requests to the same path, such as `curl http://localhost:8080/foo`
   the first request is fast and the second is delayed. For two different paths such as `/foo` and `/bar`
   both are fast.

## Implementation notes


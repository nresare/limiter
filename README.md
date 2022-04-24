# Simple http rate limiter

This small projects implements a web server that proxies GET requests to a backend.
If two requests with the same normalised path arrive closer in time than MINIMAL_GAP
milliseconds from each other the proxy delays sending subsequent requests to the backend
such that there are at least MINIMAL_GAP milliseconds between the requests.

## Running and testing

All the settings are hard coded in `WebServer.java`. A tiny echoing web server is also
included in `test_webserver.py`. I have tested this with java 17.0.3 on macOS 12.3.1

Steps to test:
1. Build and run the unit tests with `./gradlew check`
2. start the echoing webserver (listening on port 8081) in one Terminal with `./test_webserver.py`
3. run the limiter web server with `./gradlew run` in another Terminal
4. send requests in a third terminal with `curl http://localhost:8080`
5. configure the MINIMAL_GAP with a request such as this one `curl http://localhost:8080/limit/4000`
   to set minimal gap to 4 seconds.
6. verify that with two subsequent requests to the same path, such as `curl http://localhost:8080/foo`
   the first request is fast and the second is delayed. For two different paths such as `/foo` and 
   `/bar` both are fast.

## Implementation notes

The Limiter class is a generic rate limiter for request/response style asynchronous services using
`CompletableFuture`. This seemed useful to be able to unit test the algorithm without needing to
bother with http specific things. The limiter can then be used to wrap arbitrary other 
implementations of `Service`. The ProxyHandler is a jetty Handler implementation that passes its
requests onto a Service implementation that asynchronously returns a `HttpResponse` given a 
`HttpRequest`. 

Limiter is designed to handle concurrent invocations but there are no doubt bugs hiding that would
surface in a highly concurrent execution environment.

The design goals have been to keep the latency as low as possible for the non-rate limited requests
and to in theory allow for massive amounts of very bursty traffic without requiring an excessive 
amount of blocked threads for the purposes of implementing the delays.

I opted to use java after having thought of the problem for a bit because it provides facilities
such as scheduled execution and highly efficient concurrent data structures such as `ConcurrentHashMap`
and `ConcurrentLinkedDeque`. In hindsight, it probably wasn't the best choice for a time constrained 
implementation effort since figuring out how to use the API for the http client and server took 
considerable time, and I would probably have arrived at the stage where I prototype a lot earlier 
working with a language such as python.

If this code were to be put in production, this is a list of things I would prioritise:

* API documentation, a lot more testing, obviously
* Limit the length of queues and the number of unique paths requested. The current implementation
  would run out of memory and die if enough requests to a single path was sent. A better failure
  mode would probably be to reject requests above a certain threshold
* It would not handle the failure mode where a sustained request of incoming requests for a 
  specific path is higher than the limiting rate. A sensible implementation would be to have
  configurable deadline of say 10 seconds or so for the backend and fast fail requests when 
  the associated queue length would be too long to be able to send the request before the deadline.
* Thinking through the algorithm, I realise that there is a race condition where a request would 
  be added to the Queue between the empty check and the call to `remove()` in `scheduleRemoveQueue()`
  that would cause a request to be dropped. It could be fixed with some heavy-handed locking,
  but I believe that the neat fix would be to put all the modifications of `states` and its 
  associated queues to a blocking queue and have a single thread make those modifications, as 
  the cases where we are imposing a delay anyway. This way the fast path that simply attempts
  to `putIfAbsent()` would be concurrent and the slow path that queues request for delayed execution
  would be done asynchronously when latency doesn't matter.
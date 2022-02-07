# curl-tool

curl-tool will make n requests to the remote host (set via '-u' argument).
n default value is 10.

To run tests and build user Maven command (it is expected that you have Maven installed):
mvn clean install

Examples of usage:
java -jar curl-tool-1.0.jar -c "PATH_TO_CURL/curl" -u "https://google.com"

As a result you'll see
"First:
 - total time - 1422
 - calculation time - 797
 Then:
 - total time - 1340,33
 - calculation time - 774,22"

"First" - first request info.
"Then" - average statistic of all next requests.

"total time" - full time of request including DNS lookup, connection to host, ssl key change, sending request, calculation time and fully getting response.
"calculation time" - the difference between the time when the first response byte is received and the time when the request is about to be sending.

Help command for more options info:
java -jar curl-tool-1.0.jar --help

Curl: https://curl.haxx.se/
Maven: https://maven.apache.org/

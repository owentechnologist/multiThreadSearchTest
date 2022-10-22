### (Performance/Response is measured from a single JVM as it executes several Threads that each perform search queries)
## This example piggy backs on top of another example that showcases how to write JSON objects into Redis, create an index and query those objects.
## To populate Redis with the data you will need for this test - you need to run that other program found here :
https://github.com/owentechnologist/jsonZewSearch

### <em>The query filters used in this sample code are Strings found in QueryStrings.properties file
### Feel free to edit those once you are familiar with the data set 
</em>

## This example allows you to test the impact of several things on performance: 
## The primary ones are: 1. number of threads 2. size of response from redis 

Arguments you can provide include:
* --host1 (the host address/endpoint of your redis instance hosting search) 
1.   example: 192.168.1.21
* --port (the port address/endpoint of your redis instance hosting search)
1. example:  12000 
* --idxname (the name of the search index or alias you want used during th testing)
1. example:  idxa_zew_events 
* --querycountperthread (The number of query executions for each thread to perform ) 
1. example:  100 
* --limitsize (The number of rows of results to marshal and return to the calling client from Redis [think pagination] ) 
1. example:  100 
* --numberofthreads (The number of threads to spin up during this test)
1. example:  50
### To invoke this class use maven like this:

```
mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host1 192.168.1.21 --port 12000 --user applicationA --password "secretpass" --idxname idxa_zew_events --querycountperthread 5 --limitsize 10 --numberofthreads 3"
```

Or if you have no user password to worry about:
```
mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host1 192.168.1.21 --port 12000 --idxname idxa_zew_events --querycountperthread 100 --limitsize 10 --numberofthreads 3"
```

## The program will run with your settings until all threads are complete and then show the avg latency per thread for the specified number of queries executed as well as the total clock time experienced by the client threads waiting for results.

## Here is a sample run:
``` 
bash-3.2$ mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host1 192.168.1.21 --port 12000 --idxname idxa_zew_events --querycountperthread 100 --limitsize 10 --numberofthreads 3"
[INFO] Scanning for projects...
[INFO] 
[INFO] -----------------< org.example:multiThreadSearchTest >------------------
[INFO] Building multiThreadSearchTest 1.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-resources-plugin:2.6:resources (default-resources) @ multiThreadSearchTest ---
[WARNING] Using platform encoding (UTF-8 actually) to copy filtered resources, i.e. build is platform dependent!
[INFO] Copying 0 resource
[INFO] 
[INFO] --- maven-compiler-plugin:3.1:compile (default-compile) @ multiThreadSearchTest ---
[INFO] Changes detected - recompiling the module!
[WARNING] File encoding has not been set, using platform encoding UTF-8, i.e. build is platform dependent!
[INFO] Compiling 1 source file to /Users/owentaylor/wip/java/multiThreadSearchTest/target/classes
[INFO] 
[INFO] --- exec-maven-plugin:3.0.0:java (default-cli) @ multiThreadSearchTest ---
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
Connecting to redis://192.168.1.21:12000
Connecting to redis://192.168.1.21:12000
Connecting to redis://192.168.1.21:12000

Each thread will execute queries using some or all of the following filters: (selected at random each time a thread fires a query)
@days:{Sat} @days:{Sun} @times:{09*} -@location:('House') 
@contact_name:(Jo* Hu*)
@cost:[-inf 5.00]

1 threads have completed their processing...
3 threads have completed their processing...
Thread #1 executed 100 queries
Thread #1 avg execution time (milliseconds) was: 27
Thread #1 total execution time (seconds) was: 2
Thread #2 executed 100 queries
Thread #2 avg execution time (milliseconds) was: 27
Thread #2 total execution time (seconds) was: 2
Thread #3 executed 100 queries
Thread #3 avg execution time (milliseconds) was: 22
Thread #3 total execution time (seconds) was: 2
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  55.024 s
[INFO] Finished at: 2022-10-21T17:32:02-05:00
[INFO] ------------------------------------------------------------------------
```
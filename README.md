## Please note this program does not load data into Redis 
## It expects you to point it at an existing data set and Search index
## ALSO PLEASE NOTE IT DOES NOT TEST SEARCH USING AGGREGATION LOGIC

### (Performance/Response is measured from a single JVM as it executes several Threads that each perform search queries)
## As mentioned - you need to provide data and a search index...
(In fact, by building your own dataset and defining and supplying an appropriate index as an argument, you can use this code to test against any index and set of data stored in RediSearch)

#### This example can piggy backs on top of another example that showcases how to write JSON objects into Redis, create an index and query those objects.
#### If you choose To populate Redis with the data from my provided JSON zoo events example  - you may run that other program found here :
https://github.com/owentechnologist/jsonZewSearch

### <em>The query filters used in this sample code are Strings found in a file called: ```QueryStrings.properties``` 
### Feel free to edit those once you are familiar with the data set 
### By default, The fields that are marshalled and returned from queries are specified in the properties files called: 
### ```SimpleReturnFields.properties``` and ```AliasedReturnFields.properties```
You may edit these files or provide your own files and pass the names of those files as args (please see the Main.java for the available args)
</em>

## This example allows you to test the impact of several things on performance: 
## The primary ones are: 
### 1. number of threads 
### 2. size of response (number of documents returned) from redis 

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
* --pausebetweenthreads (the number of milliseconds to pause before starting each new Thread)
1. example: 250
* --querystringspropfilename 
1. example:  HashQueryStrings.properties 
* --simplereturnfieldspropfilename 
1. example: HashSimpleReturnFields.properties 
* --aliasedreturnfieldspropfilename 
1. example: HashAliasedReturnFields.properties
### To invoke this class use maven like this:

```
mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host1 192.168.1.21 --port 12000 --user applicationA --password "secretpass" --idxname idxa_zew_events --querycountperthread 10 --limitsize 50 --numberofthreads 20 pausebetweenthreads 50"
```

Or if you have no user password to worry about:
```
mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host1 192.168.1.21 --port 12000 --idxname idxa_zew_events --pausebetweenthreads 50 --querycountperthread 10 --limitsize 100 --numberofthreads 100"
```

## The program will run with your settings until all threads are complete and then show the avg latency per thread for the specified number of queries executed as well as the total clock time experienced by the client threads waiting for results.
Added variable pause between query executions by each thread.  This is currently calculated as: ((pauseBetweenThreads*2)+(limitSize/10))  So, a 1000 limitsize with 50 millisecond pause between threads makes for 200 millis pause between each query execution by a thread.
The justification for this is the larger the result set, the longer a client is likely to spend processing it before fetching more

## Here is a sample run against the search index created in the JSON example found here:  https://github.com/owentechnologist/jsonZewSearch   
``` 
mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host1 192.168.1.20 --port 12000 --idxname idxa_zew_events --querycountperthread 100 --limitsize 50 --numberofthreads 3 --pausebetweenthreads 50  --querystringspropfilename QueryStrings.properties  --simplereturnfieldspropfilename SimpleReturnFields.properties --aliasedreturnfieldspropfilename AliasedReturnFields.properties" 
[INFO] Scanning for projects...
[INFO] 
[INFO] -----------------< org.example:multiThreadSearchTest >------------------
[INFO] Building multiThreadSearchTest 1.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-resources-plugin:2.6:resources (default-resources) @ multiThreadSearchTest ---
[WARNING] Using platform encoding (UTF-8 actually) to copy filtered resources, i.e. build is platform dependent!
[INFO] Copying 13 resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.1:compile (default-compile) @ multiThreadSearchTest ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- exec-maven-plugin:3.0.0:java (default-cli) @ multiThreadSearchTest ---
loading custom --idxname == idxa_zew_events
loading custom --host1 == 192.168.1.20
loading custom --port == 12000
loading custom --querycountperthread == 100
loading custom --pausebetweenthreads == 50
loading custom --numberofthreads == 3
loading custom --limitsize == 50
loading custom --simplereturnfieldspropfilename == SimpleReturnFields.properties
loading custom --aliasedreturnfieldspropfilename == AliasedReturnFields.properties
loading custom --querystringspropfilename == QueryStrings.properties
LOADING PROPERTIES FILE: QueryStrings.properties USING CLASSLOADER...
inputStream is now: java.io.BufferedInputStream@7ef72c23
! --> CLASSLOADER LOADED PROPERTIES FILE...
Connection Creation Debug --> 2
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
PERFORMANCE_TEST_THREAD_COUNTER SET TO: 0
Connecting to redis://192.168.1.21:12000
SEARCH_INDEX_INFO: 

{max_doc_id=25003, sortable_values_size_mb=1.1444549560546875, inverted_sz_mb=3.9667224884033203, indexing=0, num_records=721201, num_terms=14183, hash_indexing_failures=0, number_of_uses=1601, records_per_doc_avg=14.423443794250488, cursor_stats=[global_idle, 0, global_total, 0, index_capacity, 256, index_total, 0], percent_indexed=1, bytes_per_record_avg=5.7673370838165283, vector_index_sz_mb=0, num_docs=50002, offset_bits_per_record_avg=8.5915002822875977, offset_vectors_sz_mb=0.65598678588867188, doc_table_size_mb=4.0903663635253906, gc_stats=[bytes_collected, 0], offsets_per_term_avg=0.88809505105018616, key_table_size_mb=1.6594486236572266, total_inverted_index_blocks=19027, attributes=[[identifier, $.name, attribute, event_name, type, TEXT, WEIGHT, 1], [identifier, $.cost, attribute, cost, type, NUMERIC, SORTABLE, UNF], [identifier, $.days.*, attribute, days, type, TAG, SEPARATOR, ], [identifier, $.times[*].military, attribute, times, type, TAG, SEPARATOR, ], [identifier, $.location, attribute, location, type, TEXT, WEIGHT, 1], [identifier, $.responsible_parties.hosts[*].name, attribute, contact_name, type, TEXT, WEIGHT, 0.75]], index_name=idx_zew_events, index_definition=[key_type, JSON, prefixes, [zew:activities:], default_score, 1], index_options=[]}


LOADING PROPERTIES FILE: SimpleReturnFields.properties USING CLASSLOADER...
inputStream is now: java.io.BufferedInputStream@27916cc1
! --> CLASSLOADER LOADED PROPERTIES FILE...
LOADING PROPERTIES FILE: AliasedReturnFields.properties USING CLASSLOADER...
inputStream is now: java.io.BufferedInputStream@7a99b4a
! --> CLASSLOADER LOADED PROPERTIES FILE...
Connecting to redis://192.168.1.20:12000
Connecting to redis://192.168.1.21:12000

Each thread will execute queries using some or all of the following filters: (selected at random each time a thread fires a query)
@days:{Sat} @days:{Sun} @times:{09*} -@location:('House')
@contact_name:(Jo* Hu*)
@cost:[-inf 5.00]
@cost:[0.00 0.00]
@contact_name:(Vi* MD*)
@location:('Gorilla House South')
@event_name:(Lla* Do*)
Meerkat MD House East
@cost:[25 25] Petting MD
Waiting for results to come in from our threads...   
queryArgs == @cost:[0.00 0.00]
returnFieldsArgs[0] == location

16699 results matched -- sample matching document returned: 
zew:activities:49727
contact_phone 113.880.4788
event_time_civilian 2:00 PM
event_name Alligator Training
days ["Mon","Tue","Wed","Fri","Sat"]
location Alligator Area East
contact_email Santo@zew.org
event_time_military 1400
queryArgs == Meerkat MD House East
returnFieldsArgs[0] == location

4 results matched -- sample matching document returned: 
zew:activities:24487
contact_phone 144.145.4358
event_time_civilian 10:00 PM
event_name Meerkat Lecture
days ["Mon","Tue","Wed","Thu","Fri","Sat"]
location Meerkat House East
contact_email Tyler@zew.org
event_time_military 2200
queryArgs == @cost:[0.00 0.00]
returnFieldsArgs[0] == location

16699 results matched -- sample matching document returned: 
zew:activities:49727
contact_phone 113.880.4788
event_time_civilian 2:00 PM
event_name Alligator Training
days ["Mon","Tue","Wed","Fri","Sat"]
location Alligator Area East
contact_email Santo@zew.org
event_time_military 1400
.....
It's been 12508 milliseconds since this program launched and RESULTS SO FAR -->>  
3 threads have completed their processing...
Throughput per second for the executed Search Queries is approximately: 25

The program will now Tally up the times taken by each query for each Thread and provide a summary.


Thread #1 executed 100 queries
Thread #1 avg execution time (milliseconds) was: 6
Thread #1 total execution time (milliseconds) was: 651
Thread #2 executed 100 queries
Thread #2 avg execution time (milliseconds) was: 6
Thread #2 total execution time (milliseconds) was: 633
Thread #3 executed 100 queries
Thread #3 avg execution time (milliseconds) was: 5
Thread #3 total execution time (milliseconds) was: 593

Across 123 unique results captured, latencies look like this:
Lowest Recorded roundtrip: [Thread #1: executed query: Meerkat MD House East (with 4 results and limit size of 50) Execution took: 2 milliseconds]
5th percentile: [Thread #2: executed query: @contact_name:(Vi* MD*) (with 17 results and limit size of 50) Execution took: 3 milliseconds]
10th percentile: [Thread #1: executed query: @contact_name:(Jo* Hu*) (with 44 results and limit size of 50) Execution took: 4 milliseconds]
25th percentile: [Thread #2: executed query: @event_name:(Lla* Do*) (with 455 results and limit size of 50) Execution took: 5 milliseconds]
50th percentile: [Thread #1: executed query: @days:{Sat} @days:{Sun} @times:{09*} -@location:('House') (with 1105 results and limit size of 50) Execution took: 7 milliseconds]
75th percentile: [Thread #1: executed query: @cost:[-inf 5.00] (with 33622 results and limit size of 50) Execution took: 9 milliseconds]
90th percentile: [Thread #3: executed query: @location:('Gorilla House South') (with 129 results and limit size of 50) Execution took: 10 milliseconds]
95th percentile: [Thread #3: executed query: @cost:[0.00 0.00] (with 16699 results and limit size of 50) Execution took: 12 milliseconds]
Highest Recorded roundtrip: [Thread #1: executed query: @cost:[0.00 0.00] (with 16699 results and limit size of 50) Execution took: 18 milliseconds]

        Throughput per second for the executed Search Queries is approximately: 25

Please check the --> slowlog <-- on your Redis database to determine if any slowness is serverside or driven by client or network limits


[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  13.330 s
[INFO] Finished at: 2023-05-30T05:15:05-05:00
[INFO] ------------------------------------------------------------------------

```

### Here is a sample run against the CitySearch IDX created as part of the example found here: https://github.com/owentechnologist/WebRateLimiter  
``` 
mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host redis-14787.homelab.local --port 14787 --idxname IDX_cities --querycountperthread 100 --limitsize 10 --numberofthreads 100 --pausebetweenthreads 100 --querystringspropfilename CitySearchQueryStrings.properties  --simplereturnfieldspropfilename CitySearchSimpleReturnFields.properties --aliasedreturnfieldspropfilename CitySearchAliasedReturnFields.properties"
```

### And the end results: 
``` 
Across 5476 unique results captured, latencies look like this:
Lowest Recorded roundtrip: [Thread #100: executed query: @zip_codes_or_postal_codes:(POT) (with 0 results and limit size of 10) Execution took: 1 milliseconds]
5th percentile: [Thread #12: executed query: @city:(Pel*) (with 3 results and limit size of 10) Execution took: 2 milliseconds]
10th percentile: [Thread #32: executed query: @zip_codes_or_postal_codes:(P7A|V0J|N6H|N6P) (with 11 results and limit size of 10) Execution took: 2 milliseconds]
25th percentile: [Thread #95: executed query: @zip_codes_or_postal_codes:(E8J) (with 2 results and limit size of 10) Execution took: 2 milliseconds]
50th percentile: [Thread #15: executed query: @city:(Pe*) @geopoint:[-72.05,45.53,500,km] (with 12 results and limit size of 10) Execution took: 4 milliseconds]
75th percentile: [Thread #72: executed query: @zip_codes_or_postal_codes:(R4L) (with 1 results and limit size of 10) Execution took: 5 milliseconds]
90th percentile: [Thread #77: executed query: @city:(Pel*) (with 3 results and limit size of 10) Execution took: 8 milliseconds]
95th percentile: [Thread #53: executed query: @zip_codes_or_postal_codes:(POT) (with 0 results and limit size of 10) Execution took: 12 milliseconds]
Highest Recorded roundtrip: [Thread #35: executed query: @zip_codes_or_postal_codes:(R4L) (with 1 results and limit size of 10) Execution took: 74 milliseconds]

        Throughput per second for the executed Search Queries is approximately: 285

Please check the --> slowlog <-- on your Redis database to determine if any slowness is serverside or driven by client or network limits
```
### (Performance/Response is measured from a single JVM as it executes several Threads that each perform search queries)
## This example piggy backs on top of another example that showcases how to write JSON objects into Redis, create an index and query those objects.
## To populate Redis with the data you will need for this test - you need to run that other program found here :
https://github.com/owentechnologist/jsonZewSearch

### <em>The query filters used in this sample code are Strings found in a file called: ```QueryStrings.properties``` 
### Feel free to edit those once you are familiar with the data set 
(In fact, by building your own dataset and defining and supplying an appropriate index as an argument, you can use this code to test against any index and set of data stored in RediSearch)
### The fields that are marshalled and returned from queries are specified in the properties files called: 
### ```SimpleReturnFields.properties``` and ```AliasedReturnFields.properties```
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
### To invoke this class use maven like this:

```
mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host1 192.168.1.21 --port 12000 --user applicationA --password "secretpass" --idxname idxa_zew_events --querycountperthread 5 --limitsize 10 --numberofthreads 3 pausebetweenthreads 200"
```

Or if you have no user password to worry about:
```
mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host1 192.168.1.21 --port 12000 --idxname idxa_zew_events --pausebetweenthreads 250 --querycountperthread 100 --limitsize 100 --numberofthreads 100"
```

## The program will run with your settings until all threads are complete and then show the avg latency per thread for the specified number of queries executed as well as the total clock time experienced by the client threads waiting for results.

## Here is a sample run:
``` 
bash-3.2$ mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host1 192.168.1.21 --port 12000 --idxname idxa_zew_events --querycountperthread 20 --limitsize 2 --numberofthreads 5 --pausebetweenthreads 200"
[INFO] Scanning for projects...
[INFO] 
[INFO] -----------------< org.example:multiThreadSearchTest >------------------
[INFO] Building multiThreadSearchTest 1.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-resources-plugin:2.6:resources (default-resources) @ multiThreadSearchTest ---
[WARNING] Using platform encoding (UTF-8 actually) to copy filtered resources, i.e. build is platform dependent!
[INFO] Copying 3 resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.1:compile (default-compile) @ multiThreadSearchTest ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- exec-maven-plugin:3.0.0:java (default-cli) @ multiThreadSearchTest ---
LOADING PROPERTIES FILE: QueryStrings.properties USING CLASSLOADER...
inputStream is now: java.io.BufferedInputStream@4b64840a
! --> CLASSLOADER LOADED PROPERTIES FILE...
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
Connecting to redis://192.168.1.21:12000
SEARCH_INDEX_INFO: 

{max_doc_id=250008, sortable_values_size_mb=22.888229370117188, inverted_sz_mb=61.374286651611328, indexing=0, num_records=11586972, num_terms=28410, hash_indexing_failures=0, records_per_doc_avg=11.586948871612549, cursor_stats=[global_idle, 0, global_total, 0, index_capacity, 512, index_total, 0], percent_indexed=1, bytes_per_record_avg=5.5541348457336426, vector_index_sz_mb=0, num_docs=1000002, offset_bits_per_record_avg=8, offset_vectors_sz_mb=9.4981203079223633, doc_table_size_mb=82.863866806030273, gc_stats=[bytes_collected, 0], offsets_per_term_avg=0.85954301059246063, key_table_size_mb=37.246078491210938, total_inverted_index_blocks=451438, attributes=[[identifier, $.name, attribute, event_name, type, TEXT, WEIGHT, 1], [identifier, $.cost, attribute, cost, type, NUMERIC, SORTABLE], [identifier, $.days.*, attribute, days, type, TAG, SEPARATOR, ], [identifier, $.times.*.military, attribute, times, type, TAG, SEPARATOR, ], [identifier, $.location, attribute, location, type, TEXT, WEIGHT, 1], [identifier, $.responsible-parties.[0].name, attribute, contact_name, type, TEXT, WEIGHT, 0.75]], index_name=idx_zew_events, index_definition=[key_type, JSON, prefixes, [zew:activities:], default_score, 1], index_options=[]}


LOADING PROPERTIES FILE: SimpleReturnFields.properties USING CLASSLOADER...
inputStream is now: java.io.BufferedInputStream@6be6705
! --> CLASSLOADER LOADED PROPERTIES FILE...
LOADING PROPERTIES FILE: AliasedReturnFields.properties USING CLASSLOADER...
inputStream is now: java.io.BufferedInputStream@27327d87
! --> CLASSLOADER LOADED PROPERTIES FILE...
Connecting to redis://192.168.1.21:12000
Connecting to redis://192.168.1.21:12000
Connecting to redis://192.168.1.21:12000
Connecting to redis://192.168.1.21:12000
queryArgs == @cost:[25 25] Petting MD
returnFieldsArgs[0] == location

356 results matched -- sample matching document returned: 
zew:activities:143564
contact_phone 152-618-6808
first_event_time 12:30 PM
event_name Green Anaconda Petting
days ["Tue","Wed","Fri","Sat"]
location Green Anaconda Habitat South
contact_email Felica@zew.org
queryArgs == @contact_name:(Vi* MD*)
returnFieldsArgs[0] == location

103 results matched -- sample matching document returned: 
zew:activities:339179
contact_phone (994) 143-0826
first_event_time 3:00 PM
event_name Kangaroo Feeding
days ["Tue","Wed","Fri","Sat"]
location Kangaroo Lecture Hall South
contact_email Virgie@zew.org

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
queryArgs == @contact_name:(Vi* MD*)
returnFieldsArgs[0] == location

103 results matched -- sample matching document returned: 
zew:activities:339179
contact_phone (994) 143-0826
first_event_time 3:00 PM
event_name Kangaroo Feeding
days ["Tue","Wed","Fri","Sat"]
location Kangaroo Lecture Hall South
contact_email Virgie@zew.org
queryArgs == @cost:[0.00 0.00]
returnFieldsArgs[0] == location

332933 results matched -- sample matching document returned: 
zew:activities:12417
contact_phone 381-473-6763
first_event_time 6:00 PM
event_name Flamingo Ride
days ["Mon","Tue","Wed","Fri","Sat"]
location Flamingo Theater East
contact_email Emory@zew.org
queryArgs == @location:('Gorilla House South')
returnFieldsArgs[0] == location

2362 results matched -- sample matching document returned: 
zew:activities:177805
contact_phone (889) 805-2974
first_event_time 12:30 PM
event_name Gorilla Documentary
days ["Mon","Tue","Wed","Thu","Fri","Sun"]
location Gorilla House South
contact_email Mrs.@zew.org


RESULTS ARE IN!-->>  3 threads have completed their processing...


RESULTS ARE IN!-->>  5 threads have completed their processing...
Thread #1 executed 20 queries
Thread #1 avg execution time (milliseconds) was: 10
Thread #1 total execution time (milliseconds) was: 202
Thread #2 executed 20 queries
Thread #2 avg execution time (milliseconds) was: 11
Thread #2 total execution time (milliseconds) was: 232
Thread #3 executed 20 queries
Thread #3 avg execution time (milliseconds) was: 12
Thread #3 total execution time (milliseconds) was: 242
Thread #4 executed 20 queries
Thread #4 avg execution time (milliseconds) was: 12
Thread #4 total execution time (milliseconds) was: 240
Thread #5 executed 20 queries
Thread #5 avg execution time (milliseconds) was: 10
Thread #5 total execution time (milliseconds) was: 213
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  12.856 s
[INFO] Finished at: 2022-10-23T02:34:15-05:00
[INFO] ------------------------------------------------------------------------
bash-3.2$ 
```
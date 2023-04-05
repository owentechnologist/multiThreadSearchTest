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

## Here is a sample run:
``` 
bash-3.2$ mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host1 192.168.1.21 --port 10400 --idxname idxa_zew_events --querycountperthread 200 --limitsize 250 --numberofthreads 5 --pausebetweenthreads 250"
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
inputStream is now: java.io.BufferedInputStream@3778391d
! --> CLASSLOADER LOADED PROPERTIES FILE...
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
Connecting to redis://192.168.1.21:10400
SEARCH_INDEX_INFO: 

{max_doc_id=1000003, sortable_values_size_mb=45.7763671875, inverted_sz_mb=159.25590515136719, indexing=0, num_records=28866626, num_terms=14204, hash_indexing_failures=0, number_of_uses=733, records_per_doc_avg=14.433312892913818, cursor_stats=[global_idle, 0, global_total, 0, index_capacity, 256, index_total, 0], percent_indexed=1, bytes_per_record_avg=5.7849476337432861, vector_index_sz_mb=0, num_docs=2000000, offset_bits_per_record_avg=8.5933132171630859, offset_vectors_sz_mb=26.269218444824219, doc_table_size_mb=166.78704833984375, gc_stats=[bytes_collected, 0], offsets_per_term_avg=0.88834241032600403, key_table_size_mb=64.653835296630859, total_inverted_index_blocks=287302, attributes=[[identifier, $.name, attribute, event_name, type, TEXT, WEIGHT, 1], [identifier, $.cost, attribute, cost, type, NUMERIC, SORTABLE, UNF], [identifier, $.days.*, attribute, days, type, TAG, SEPARATOR, ], [identifier, $.times[*].military, attribute, times, type, TAG, SEPARATOR, ], [identifier, $.location, attribute, location, type, TEXT, WEIGHT, 1], [identifier, $.responsible-parties.hosts[*].name, attribute, contact_name, type, TEXT, WEIGHT, 0.75]], index_name=idx_zew_events, index_definition=[key_type, JSON, prefixes, [zew:activities:], default_score, 1], index_options=[]}


LOADING PROPERTIES FILE: SimpleReturnFields.properties USING CLASSLOADER...
inputStream is now: java.io.BufferedInputStream@5147626c
! --> CLASSLOADER LOADED PROPERTIES FILE...
LOADING PROPERTIES FILE: AliasedReturnFields.properties USING CLASSLOADER...
inputStream is now: java.io.BufferedInputStream@7f03a089
! --> CLASSLOADER LOADED PROPERTIES FILE...
Connecting to redis://192.168.1.21:10400
Connecting to redis://192.168.1.21:10400
Connecting to redis://192.168.1.21:10400
Connecting to redis://192.168.1.21:10400
queryArgs == @contact_name:(Vi* MD*)
returnFieldsArgs[0] == location

991 results matched -- sample matching document returned: 
zew:activities:1158470
contact_phone (504) 670-4944
first_event_time_military 2200
event_name Bonobo Training
days ["Mon","Tue","Wed","Fri","Sat"]
location Bonobo Theater West
first_event_time_civilian 10:00 PM
contact_email Gale@zew.org
queryArgs == @cost:[0.00 0.00]
returnFieldsArgs[0] == location

668263 results matched -- sample matching document returned: 
zew:activities:1000143
contact_phone 1-260-678-8957
first_event_time_military 2000
event_name Hyena Documentary
days ["Tue","Wed","Fri","Sat"]
location Hyena Habitat East
first_event_time_civilian 8:00 PM
contact_email Jaleesa@zew.org

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
queryArgs == @location:('Gorilla House South')
returnFieldsArgs[0] == location

4756 results matched -- sample matching document returned: 
zew:activities:1000327
contact_phone 1-680-895-5143
first_event_time_military 1430
event_name Gorilla Training
days ["Mon","Tue","Wed","Thu","Fri","Sat"]
location Gorilla House South
first_event_time_civilian 2:30 PM
contact_email Matthew@zew.org
queryArgs == @contact_name:(Jo* Hu*)
returnFieldsArgs[0] == location

2095 results matched -- sample matching document returned: 
zew:activities:1354072
contact_phone (749) 291-0915
first_event_time_military 2200
event_name Green Anaconda Lecture
days ["Mon","Tue","Wed","Thu","Fri","Sat"]
location Green Anaconda Habitat North
first_event_time_civilian 10:00 PM
contact_email Josef@zew.org
queryArgs == @contact_name:(Vi* MD*)
returnFieldsArgs[0] == location

991 results matched -- sample matching document returned: 
zew:activities:1158470
contact_phone (504) 670-4944
first_event_time_military 2200
event_name Bonobo Training
days ["Mon","Tue","Wed","Fri","Sat"]
location Bonobo Theater West
first_event_time_civilian 10:00 PM
contact_email Gale@zew.org
.........................................................
RESULTS COMING IN!-->>  2 threads have completed their processing...

RESULTS COMING IN!-->>  5 threads have completed their processing...
Thread #1 executed 200 queries
Thread #1 avg execution time (milliseconds) was: 60
Thread #1 total execution time (seconds) was: 12
Thread #2 executed 200 queries
Thread #2 avg execution time (milliseconds) was: 62
Thread #2 total execution time (seconds) was: 12
Thread #3 executed 200 queries
Thread #3 avg execution time (milliseconds) was: 66
Thread #3 total execution time (seconds) was: 13
Thread #4 executed 200 queries
Thread #4 avg execution time (milliseconds) was: 63
Thread #4 total execution time (seconds) was: 12
Thread #5 executed 200 queries
Thread #5 avg execution time (milliseconds) was: 68
Thread #5 total execution time (seconds) was: 13

Across 799 unique results captured, latencies look like this:
Lowest Recorded roundtrip: [Thread #1: executed query: Meerkat MD House East (with 132 results and limit size of 250) Execution took: 14 milliseconds]
5th percentile: [Thread #4: executed query: @contact_name:(Vi* MD*) (with 991 results and limit size of 250) Execution took: 27 milliseconds]
10th percentile: [Thread #2: executed query: @cost:[25 25] Petting MD (with 1419 results and limit size of 250) Execution took: 32 milliseconds]
25th percentile: [Thread #4: executed query: @event_name:(Lla* Do*) (with 18059 results and limit size of 250) Execution took: 40 milliseconds]
50th percentile: [Thread #4: executed query: @days:{Sat} @days:{Sun} @times:{09*} -@location:('House') (with 45656 results and limit size of 250) Execution took: 58 milliseconds]
75th percentile: [Thread #5: executed query: @location:('Gorilla House South') (with 4756 results and limit size of 250) Execution took: 83 milliseconds]
90th percentile: [Thread #2: executed query: @cost:[-inf 5.00] (with 1339718 results and limit size of 250) Execution took: 123 milliseconds]
95th percentile: [Thread #1: executed query: @cost:[-inf 5.00] (with 1339718 results and limit size of 250) Execution took: 150 milliseconds]
Highest Recorded roundtrip: [Thread #2: executed query: @cost:[-inf 5.00] (with 1339718 results and limit size of 250) Execution took: 316 milliseconds]

Please check the --> slowlog <-- on your Redis database to determine if any slowness is serverside or driven by client or network limits


[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:03 min
[INFO] Finished at: 2022-11-03T17:23:49-05:00
[INFO] ------------------------------------------------------------------------
```
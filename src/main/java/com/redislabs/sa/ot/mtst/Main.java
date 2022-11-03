package com.redislabs.sa.ot.mtst;
import com.redislabs.sa.ot.util.PropertyFileFetcher;
import redis.clients.jedis.*;
import redis.clients.jedis.search.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.*;

/**
 * To invoke this class use:
 * mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host 192.168.1.21 --port 12000 --user applicationA --password "secretpass" --idxname idxa_zew_events --querycountperthread 10 --limitsize 50 --numberofthreads 10 --pausebetweenthreads 50"
 * mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host 192.168.1.21 --port 12000 --idxname idxa_zew_events --querycountperthread 10 --limitsize 100 --numberofthreads 10 --pausebetweenthreads 50"
 */
public class Main {
    static String PERFORMANCE_TEST_THREAD_COUNTER = "PERFORMANCE_TEST_THREAD_COUNTER";
    static String ALL_RESULTS_SORTED_SET="allresults";
    static String INDEX_ALIAS_NAME = "idxa_zew_events";
    public static void main(String[] args){
        String host1 = "192.168.1.20";
        String host2 = "192.168.1.21";
        int port = 12000;
        String username = "default";
        String password = "";
        int limitSize = 1000;
        int numberOfThreads = 100;
        int queryCountPerThread = 100;
        int pauseBetweenThreads = 100;//milliseconds
        ArrayList<String> argList =null;
        ArrayList<SearchTest> testers = new ArrayList<>();
        ArrayList<String> searchQueries = loadSearchQueries();

        if(args.length>0){
            argList = new ArrayList<>(Arrays.asList(args));
            if(argList.contains("--idxname")){
                int idxNameIndex = argList.indexOf("--idxname");
                INDEX_ALIAS_NAME = argList.get(idxNameIndex+1);
            }
            if(argList.contains("--host1")){
                int host1Index = argList.indexOf("--host1");
                host1 = argList.get(host1Index+1);
            }
            if(argList.contains("--host2")){
                int host2Index = argList.indexOf("--host2");
                host2 = argList.get(host2Index+1);
            }
            if(argList.contains("--port")){
                int portIndex = argList.indexOf("--port");
                port = Integer.parseInt(argList.get(portIndex+1));
            }
            if(argList.contains("--querycountperthread")){
                int queryCountIndex = argList.indexOf("--querycountperthread");
                queryCountPerThread = Integer.parseInt(argList.get(queryCountIndex+1));
            }
            if(argList.contains("--pausebetweenthreads")){
                int pauseIndex = argList.indexOf("--pausebetweenthreads");
                pauseBetweenThreads = Integer.parseInt(argList.get(pauseIndex+1));
            }
            if(argList.contains("--numberofthreads")){
                int quantityIndex = argList.indexOf("--numberofthreads");
                numberOfThreads = Integer.parseInt(argList.get(quantityIndex+1));
            }
            if(argList.contains("--limitsize")){
                int limitIndex = argList.indexOf("--limitsize");
                limitSize = Integer.parseInt(argList.get(limitIndex+1));
            }
            if(argList.contains("--username")){
                int userNameIndex = argList.indexOf("--username");
                username = argList.get(userNameIndex+1);
            }
            if(argList.contains("--password")){
                int passwordIndex = argList.indexOf("--password");
                password = argList.get(passwordIndex + 1);
            }
        }
        HostAndPort hnp1 = new HostAndPort(host1,port);
        HostAndPort hnp2 = null;
        if(argList.contains("--host2")) {
            hnp2 = new HostAndPort(host2, port);
        }else{
            hnp2 = new HostAndPort(host1, port);
        }
        URI uri1 = null;
        URI uri2 = null;
        try {
            if(!("".equalsIgnoreCase(password))){
                uri1 = new URI("redis://" + username + ":" + password + "@" + hnp1.getHost() + ":" + hnp1.getPort());
                uri2 = new URI("redis://" + username + ":" + password + "@" + hnp2.getHost() + ":" + hnp2.getPort());
            }else{
                uri1 = new URI("redis://" + hnp1.getHost() + ":" + hnp1.getPort());
                uri2 = new URI("redis://" + hnp2.getHost() + ":" + hnp2.getPort());
            }
        }catch(URISyntaxException use){use.printStackTrace();System.exit(1);}
        //Have to do this before the test kicks off!
        Jedis jedis = new Jedis(uri1);
        jedis.set(PERFORMANCE_TEST_THREAD_COUNTER,"0");
        jedis.del(ALL_RESULTS_SORTED_SET);

        URI choice = uri1; //99% of the time you only want a single target redis uri
        // - the following bit of code does nothing in that case
        for(int x= 0;x<numberOfThreads;x++){
            choice = uri1;
            if(x%2==0){
                choice=uri2;
            }
            System.out.println("Connecting to "+choice.toString());
            SearchTest test = new SearchTest();
            test.setIndexAliasName(INDEX_ALIAS_NAME);
            test.setTestInstanceID("#"+(x+1));
            test.setUri(choice);
            test.setNumberOfResultsLimit(limitSize);
            test.setTimesToQuery(queryCountPerThread);
            test.setMillisecondPauseBetweenQueryExecutions((pauseBetweenThreads*2)+(limitSize/10)); // this seems reasonable to me as clients getting large results back will take more time to process them before issuing new queries- adjust if you need to
            test.setSearchQueries(searchQueries);
            test.init(); // get jedis connection for the thread
            testers.add(test);
        }
        for(SearchTest test:testers){
            try {
                Thread.sleep(pauseBetweenThreads);
            }catch(Throwable t){}
            Thread t = new Thread(test);
            t.start();
        }
        System.out.println("\nEach thread will execute queries using some or all of the following filters: (selected at random each time a thread fires a query)");
        for(String q: searchQueries){
            System.out.println(q);
        }
        //wait to determine test has ended before getting results:
        waitForCompletion(false,uri1,testers.size());

        for(SearchTest test:testers){
            ArrayList<Long> numericResults = test.getPerfTestNumericResults();
            ArrayList<String> stringResults = test.getPerfTestResults();
            String threadId = "Thread "+stringResults.get(0).split(":")[0];
            long totalMilliseconds =0l;
            long avgDuration = 0l;
            int resultsCounter =0;
            for(Long time:numericResults){
                totalMilliseconds+=time;
                jedis.zadd(ALL_RESULTS_SORTED_SET,time,"Thread "+stringResults.get(resultsCounter));
                resultsCounter++;
            }
            avgDuration = totalMilliseconds/numericResults.size();
            System.out.println(threadId+" executed "+numericResults.size()+" queries");
            System.out.println(threadId+" avg execution time (milliseconds) was: "+avgDuration);
            if(totalMilliseconds>1000) {
                System.out.println(threadId + " total execution time (seconds) was: " + totalMilliseconds / 1000);
            }else{
                System.out.println(threadId + " total execution time (milliseconds) was: " + totalMilliseconds);
            }
        }
        long totalResultsCaptured = jedis.zcard(ALL_RESULTS_SORTED_SET);
        System.out.println("\nAcross "+totalResultsCaptured+" unique results captured, latencies look like this:");

        System.out.println("Lowest Recorded roundtrip: "+jedis.zrange(ALL_RESULTS_SORTED_SET,0,0));
        long millis05Index = (long) (totalResultsCaptured*.05);
        System.out.println("5th percentile: "+jedis.zrange(ALL_RESULTS_SORTED_SET,millis05Index,millis05Index));
        long millis10Index = (long) (totalResultsCaptured*.1);
        System.out.println("10th percentile: "+jedis.zrange(ALL_RESULTS_SORTED_SET,millis10Index,millis10Index));
        long millis25Index = (long) (totalResultsCaptured*.25);
        System.out.println("25th percentile: "+jedis.zrange(ALL_RESULTS_SORTED_SET,millis25Index,millis25Index));
        long millis50Index = (long) (totalResultsCaptured*.5);
        System.out.println("50th percentile: "+jedis.zrange(ALL_RESULTS_SORTED_SET,millis50Index,millis50Index));
        long millis75Index = (long) (totalResultsCaptured*.75);
        System.out.println("75th percentile: "+jedis.zrange(ALL_RESULTS_SORTED_SET,millis75Index,millis75Index));
        long millis90Index = (long) (totalResultsCaptured*.9);
        System.out.println("90th percentile: "+jedis.zrange(ALL_RESULTS_SORTED_SET,millis90Index,millis90Index));
        long millis95Index = (long) (totalResultsCaptured*.95);
        System.out.println("95th percentile: "+jedis.zrange(ALL_RESULTS_SORTED_SET,millis95Index,millis95Index));
        System.out.println("Highest Recorded roundtrip: "+jedis.zrange(ALL_RESULTS_SORTED_SET,(totalResultsCaptured-1),(totalResultsCaptured-1)));
        System.out.println("\nPlease check the --> slowlog <-- on your Redis database to determine if any slowness is serverside or driven by client or network limits\n\n");
    }

    static void waitForCompletion(boolean userDriven,URI uri, int threadsExpected){
        boolean noResultsYet = true;
        if(userDriven) {
            System.out.println("Pausing... \n\n\tPlease hit enter when all test threads have completed...");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in));
            try {
                reader.readLine();
            } catch (Throwable t) {
            }
        }else{
            Jedis jedis = new Jedis(uri);
            System.out.println("Waiting for results to come in from our threads...   ");
            while(noResultsYet){
                try{
                    Thread.sleep(2000);
                }catch(InterruptedException ie){ie.printStackTrace();}
                if(jedis.exists(PERFORMANCE_TEST_THREAD_COUNTER)) {
                    int threadsCompleted = Integer.parseInt(jedis.get(PERFORMANCE_TEST_THREAD_COUNTER));
                    if(threadsCompleted>0) {
                        System.out.println("\nRESULTS COMING IN!-->>  " + threadsCompleted + " threads have completed their processing...");
                    }else{
                        System.out.print(".");
                    }
                    if (threadsExpected <= threadsCompleted) {
                        noResultsYet = false;
                    }
                }
            }
        }
    }

    static ArrayList<String> loadSearchQueries(){
        Properties p = PropertyFileFetcher.loadProps("QueryStrings.properties");
        ArrayList<String> queries = new ArrayList<>();
        for(String s : p.stringPropertyNames()) {
            queries.add(p.getProperty(s));
        /*queries.add("@days:{Sat} @days:{Sun} @times:{09*} -@location:('House') ");
        queries.add("@contact_name:(Jo* Hu*)");
        queries.add("@cost:[-inf 5.00]");*/
        }
        return queries;
    }
}

class SearchTest implements Runnable{
    URI uri = null;
    ArrayList<String> searchQueries = null;
    ArrayList<String> perfTestResults = new ArrayList<>();
    ArrayList<Long> perfTestNumericResults = new ArrayList<>();
    String indexAliasName = "";
    JedisPooled pool = null;
    int timesToQuery =1;
    int numberOfResultsLimit = 100;
    long millisecondPauseBetweenQueryExecutions = 500;
    String testInstanceID = "";
    static boolean needToLoadFields=true;
    static ArrayList<FieldName> fieldsReturned = new ArrayList<>();
    boolean showSample = true;
    static boolean showSearchIndexInfo = true;


    static ConnectionPoolConfig connectionPoolConfig;
    {
        connectionPoolConfig = new ConnectionPoolConfig();
        connectionPoolConfig.setMaxIdle(1000);
        connectionPoolConfig.setMaxTotal(1000);
        connectionPoolConfig.setMaxWait(Duration.ofSeconds(300));
        connectionPoolConfig.setMinIdle(100);
        connectionPoolConfig.setTestOnReturn(true);
        connectionPoolConfig.setTestOnCreate(true);//extra ping
    }

    public void setMillisecondPauseBetweenQueryExecutions(long millisecondPauseBetweenQueryExecutions){
        this.millisecondPauseBetweenQueryExecutions = millisecondPauseBetweenQueryExecutions;
    }

    public void setTestInstanceID(String testInstanceID) {
        this.testInstanceID = testInstanceID;
    }

    public void setSearchQueries(ArrayList<String> searchQueries) {
        this.searchQueries = searchQueries;
    }

    public void setIndexAliasName(String indexAliasName) {
        this.indexAliasName = indexAliasName;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public void setNumberOfResultsLimit(int numberOfResultsLimit) {
        this.numberOfResultsLimit = numberOfResultsLimit;
    }

    public void setTimesToQuery(int timesToQuery) {
        this.timesToQuery = timesToQuery;
    }

    public void init(){
        pool = new JedisPooled(connectionPoolConfig, uri, 120000);
        if(showSearchIndexInfo){
            System.out.println("SEARCH_INDEX_INFO: \n\n"+pool.ftInfo(indexAliasName)+"\n\n");
            showSearchIndexInfo=false; // only show it once across all threads
        }
        if(needToLoadFields){
            Properties simpleFields = PropertyFileFetcher.loadProps("SimpleReturnFields.properties");
            for(String f : simpleFields.stringPropertyNames()){
                fieldsReturned.add(FieldName.of(simpleFields.getProperty(f)));
            }
            Properties aliasedFields = PropertyFileFetcher.loadProps("AliasedReturnFields.properties");

            for(String f : aliasedFields.stringPropertyNames()){
                fieldsReturned.add(FieldName.of(aliasedFields.getProperty(f).split(":")[0]).as(aliasedFields.getProperty(f).split(":")[1]));
            }
            needToLoadFields=false;
        }
    }

    //default constructor initializes the object
    public SearchTest(){
    }

    @Override
    public void run() {
        for(int x=0;x<timesToQuery;x++){
            try {
                Thread.sleep(millisecondPauseBetweenQueryExecutions);
                executeQueryLoadedReturnFields();
            }catch(InterruptedException ie){ie.getMessage();}
        }
        pool.incr(Main.PERFORMANCE_TEST_THREAD_COUNTER);
        //System.out.println("ThreadID: "+testInstanceID+" COMPLETED TEST RUN OF: "+timesToQuery+" QUERIES.");
        //System.out.println("PERFORMANCE_TEST_THREAD_COUNTER now equals: "+pool.get(Main.PERFORMANCE_TEST_THREAD_COUNTER));
    }

    ArrayList<String> getPerfTestResults(){
        return perfTestResults;
    }
    ArrayList<Long> getPerfTestNumericResults(){
        return perfTestNumericResults;
    }

    void executeQuery(){
        long startTime = System.currentTimeMillis();
        int queryIndex = (int) (System.currentTimeMillis()%searchQueries.size());
        String query = searchQueries.get(queryIndex);
        SearchResult result = pool.ftSearch(indexAliasName, new Query(query)
                .returnFields(
                        FieldName.of("location"), // only a single value exists in a document
                        FieldName.of("$.times.*.civilian").as("first_event_time"), // only returning 1st time in array due to use of *
                        FieldName.of("$.days").as("days"), // multiple days may be returned
                        FieldName.of("$.responsible-parties.hosts.[0].email").as("contact_email"), // Returning the first email only even though there could be more
                        FieldName.of("$.responsible-parties.hosts.[0].phone").as("contact_phone"), // Returning the first phone only even though there could be more
                        FieldName.of("event_name"), // only a single value exists in a document
                        FieldName.of("$.times[2].military").as("military1"), // only returning 1st time in array due to use of *
                        FieldName.of("$.description")
                ).limit(0,numberOfResultsLimit)
        );
        long duration = (System.currentTimeMillis()-startTime);
        perfTestResults.add(testInstanceID+": executed query: "+query+" (with "+result.getTotalResults()+" results and limit size of "+numberOfResultsLimit+") Execution took: "+duration+" milliseconds");
        perfTestNumericResults.add(duration);
    }

    void executeQueryLoadedReturnFields(){
        long startTime = System.currentTimeMillis();
        int queryIndex = (int) (System.nanoTime()%searchQueries.size());
        String query = searchQueries.get(queryIndex);
        FieldName[] returnFieldsArg = new FieldName[fieldsReturned.size()];
        int counter = 0;
        for (Iterator<FieldName> it = fieldsReturned.iterator(); it.hasNext(); ) {
            returnFieldsArg[counter] =  it.next();
            counter++;
        }
        SearchResult result = pool.ftSearch(indexAliasName, new Query(query)
                .returnFields(returnFieldsArg)
                .limit(0,numberOfResultsLimit));
        if(showSample) {
            //testing query results:
            System.out.println("queryArgs == "+query);
            System.out.println("returnFieldsArgs[0] == "+returnFieldsArg[0]);
            String output = "sample matching document returned: \n" + result.getDocuments().get(0).getId();
            for (Map.Entry<String, Object> e : result.getDocuments().get(0).getProperties()) {
                output += "\n" + e.getKey() + " " + e.getValue();
            }
            System.out.println("\n"+result.getTotalResults() + " results matched -- " + output);
            showSample=false;
        }

        long duration = (System.currentTimeMillis()-startTime);
        perfTestResults.add(testInstanceID+": executed query: "+query+" (with "+result.getTotalResults()+" results and limit size of "+numberOfResultsLimit+") Execution took: "+duration+" milliseconds");
        perfTestNumericResults.add(duration);
    }

}

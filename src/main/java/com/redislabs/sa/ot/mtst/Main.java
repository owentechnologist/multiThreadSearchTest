package com.redislabs.sa.ot.mtst;
import redis.clients.jedis.*;
import redis.clients.jedis.search.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * To invoke this class use:
 * mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host 192.168.1.21 --port 12000 --user applicationA --password "secretpass" --idxname idxa_zew_events --querycountperthread 5 --limitsize 2 --numberofthreads 3"
 * mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host 192.168.1.21 --port 12000 --idxname idxa_zew_events --querycountperthread 5 --limitsize 2 --numberofthreads 3"
 */
public class Main {
    static String PERFORMANCE_TEST_THREAD_COUNTER = "PERFORMANCE_TEST_THREAD_COUNTER";
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
        ArrayList<String> argList =null;
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

        ArrayList<SearchTest> testers = new ArrayList<>();
        URI choice = uri1;
        for(int x= 0;x<numberOfThreads;x++){
            choice = uri1;
            if(x%2==0){
                choice=uri2;
            }
            try {
                Thread.sleep(10);
            }catch(Throwable t){}
            System.out.println("Connecting to "+choice.toString());
            SearchTest test = new SearchTest();
            test.setIndexAliasName(INDEX_ALIAS_NAME);
            test.setTestInstanceID("#"+(x+1));
            test.setUri(choice);
            test.setNumberOfResultsLimit(limitSize);
            test.setTimesToQuery(queryCountPerThread);
            test.setSearchQueries(createSearchQueries());// need to create these
            test.init(); // get jedis connection for the thread
            testers.add(test);
        }
        for(SearchTest test:testers){
            Thread t = new Thread(test);
            t.start();
        }
        System.out.println("\nEach thread will execute queries using some or all of the following filters: (selected at random each time a thread fires a query)");
        for(String q: createSearchQueries()){
            System.out.println(q);
        }
        System.out.println("");//extra space on screen
        //wait to determine test has ended before getting results:
        waitForCompletion(false,uri1,testers.size());

        for(SearchTest test:testers){
            ArrayList<Long> numericResults = test.getPerfTestNumericResults();
            ArrayList<String> stringResults = test.getPerfTestResults();
            long totalMilliseconds =0l;
            long avgDuration = 0l;
            for(Long time:numericResults){
                totalMilliseconds+=time;
            }
            avgDuration = totalMilliseconds/numericResults.size();
            String threadId = "Thread "+stringResults.get(0).split(":")[0];
            System.out.println(threadId+" executed "+numericResults.size()+" queries");
            System.out.println(threadId+" avg execution time (milliseconds) was: "+avgDuration);
            if(totalMilliseconds>1000) {
                System.out.println(threadId + " total execution time (seconds) was: " + totalMilliseconds / 1000);
            }else{
                System.out.println(threadId + " total execution time (milliseconds) was: " + totalMilliseconds);
            }
        }

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
            jedis.del(PERFORMANCE_TEST_THREAD_COUNTER);
            while(noResultsYet){
                try{
                    Thread.sleep(1000);
                }catch(InterruptedException ie){}
                if(jedis.exists(PERFORMANCE_TEST_THREAD_COUNTER)) {
                    int threadsCompleted = Integer.parseInt(jedis.get(PERFORMANCE_TEST_THREAD_COUNTER));
                    System.out.println(threadsCompleted+" threads have completed their processing...");
                    if (threadsExpected <= threadsCompleted) {
                        noResultsYet = false;
                    }
                }
            }
        }
    }

    static ArrayList<String> createSearchQueries(){
        ArrayList<String> queries = new ArrayList<>();
        queries.add("@days:{Sat} @days:{Sun} @times:{09*} -@location:('House') ");
        queries.add("@contact_name:(Jo* Hu*)");
        queries.add("@cost:[-inf 5.00]");
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
    String testInstanceID = "";

    static ConnectionPoolConfig connectionPoolConfig;
    {
        connectionPoolConfig = new ConnectionPoolConfig();
        connectionPoolConfig.setMaxIdle(100);
        connectionPoolConfig.setMaxTotal(200);
        connectionPoolConfig.setMaxWait(Duration.ofSeconds(30));
        connectionPoolConfig.setTestOnBorrow(true);//extra ping
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
        pool = new JedisPooled(connectionPoolConfig, uri, 20000);
    }

    //default constructor initializes the object
    public SearchTest(){
    }

    @Override
    public void run() {
        for(int x=0;x<timesToQuery;x++){
            try {
                Thread.sleep(500);
                executeQuery();
            }catch(InterruptedException ie){}
        }
        pool.incr(Main.PERFORMANCE_TEST_THREAD_COUNTER);
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

}

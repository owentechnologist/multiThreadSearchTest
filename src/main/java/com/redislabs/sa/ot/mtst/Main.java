package com.redislabs.sa.ot.mtst;
import com.redislabs.sa.ot.util.JedisConnectionHelper;
import com.redislabs.sa.ot.util.JedisConnectionHelperSettings;
import com.redislabs.sa.ot.util.PropertyFileFetcher;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.search.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Please note - this program does not load data into Redis!!
 * It expects you to point it at an existing data set and Search index
 *
 * ALSO PLEASE NOTE IT DOES NOT TEST SEARCH USING AGGREGATION LOGIC
 *
 *  A simple way to load matching JSON-based data can be found here:
 *  https://github.com/owentechnologist/zewtopia_wrkshop_scripts/blob/master/add_json_entities_and_search.md
 *
 * To invoke this class use:
 * mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host 192.168.1.21 --port 12000 --user applicationA --password "secretpass" --idxname idxa_zew_events --querycountperthread 10 --limitsize 50 --numberofthreads 10 --pausebetweenthreads 50"
 * mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host 192.168.1.21 --port 12000 --idxname idxa_zew_events --querycountperthread 10 --limitsize 100 --numberofthreads 10 --pausebetweenthreads 50"
 *
 * NOTE THAT YOU CAN PROVIDE YOUR OWN DATASET AND INDEX ALONG WITH YOUR OWN QUERY PARAMS
 *
 *  * It supports JSON search indexes and datasets but can be customized to query for Hash-based simple fields as well
 *
 * If you need to specify non-default properties files for dynamic loading of query parameters and result parsing use:
 * mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host 192.168.1.20 --port 10900 --idxname idx_omine --querycountperthread 10 --limitsize 100 --numberofthreads 10 --pausebetweenthreads 50 --querystringspropfilename idx_omineQueryStrings.properties  --simplereturnfieldspropfilename idx_omineSimpleReturnFields.properties --aliasedreturnfieldspropfilename idx_omineAliasedReturnFields.properties"
 *
 * Sample Hash datatype compatible properties files are found in the resources folder with the prefix 'Hash'
 * You can invoke this program by using those alternate properties like this:
 * mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host 192.168.1.21 --port 14787 --idxname idxa_zew --querycountperthread 10 --limitsize 100 --numberofthreads 10 --pausebetweenthreads 50 --querystringspropfilename HashQueryStrings.properties  --simplereturnfieldspropfilename HashSimpleReturnFields.properties --aliasedreturnfieldspropfilename HashAliasedReturnFields.properties"
 *
 *  A simple way to load matching Hash-based data can be found here:
 *  https://github.com/owentechnologist/zewtopia_wrkshop_scripts/blob/master/populate_zew_animals.lua.md
 *
 */
public class Main {
    static String queryStringsPropFileName = "idxa_zew_eventsQueryStrings.properties";
    static String simpleReturnFieldsPropFileName = "idxa_zew_eventsSimpleReturnFields.properties";
    static String aliasedReturnFieldsPropFileName = "idxa_zew_eventsAliasedReturnFields.properties";
    static String PERFORMANCE_TEST_THREAD_COUNTER = "PERFORMANCE_TEST_THREAD_COUNTER";
    static String ALL_RESULTS_SORTED_SET="allresults";
    static String INDEX_ALIAS_NAME = "idxa_zew_events";
    static long systemTestStartTime = System.currentTimeMillis();
    private static boolean multiValueSearch = false;
    public static int dialectVersion = 2;//Dialect 3 is needed for complete multivalue results
    public static int queryCountPerThread = 100;
    static JedisConnectionHelper connectionHelper = null;

    public static void main(String[] args){

        String host = "localhost";
        String host1 = "192.168.1.20";
        // This second host is not normally utilized:
        String host2 = "192.168.1.21";
        int port = 12000;
        String userName = "default";
        String password = "";
        int limitSize = 1000;
        int numberOfThreads = 100;
        int pauseBetweenThreads = 100;//milliseconds
        boolean useSSL = false;
        String caCertPath = "";
        String caCertPassword = "";
        String userCertPath = "";
        String userCertPassword = "";
        int maxConnections = 1;
        ArrayList<String> argList =null;
        ArrayList<SearchTest> testers = new ArrayList<>();
        ArrayList<String> searchQueries = null;

        if(args.length>0){
            argList = new ArrayList<>(Arrays.asList(args));
            if(argList.contains("--idxname")){
                int argIndex = argList.indexOf("--idxname");
                INDEX_ALIAS_NAME = argList.get(argIndex+1);
                System.out.println("loading custom --idxname == "+INDEX_ALIAS_NAME);
                queryStringsPropFileName = INDEX_ALIAS_NAME+"QueryStrings.properties";
                simpleReturnFieldsPropFileName = INDEX_ALIAS_NAME+"SimpleReturnFields.properties";
                aliasedReturnFieldsPropFileName = INDEX_ALIAS_NAME+"AliasedReturnFields.properties";
            }
            if(argList.contains("--multivalue")){
                int argIndex = argList.indexOf("--host2");
                multiValueSearch = Boolean.parseBoolean(argList.get(argIndex+1));
                if(multiValueSearch){
                    dialectVersion=3;
                }
            }
            if(argList.contains("--host")){
                int argIndex = argList.indexOf("--host");
                host = argList.get(argIndex+1);
                System.out.println("loading custom --host == "+host1);
            }
            if(argList.contains("--host1")){
                int argIndex = argList.indexOf("--host1");
                host1 = argList.get(argIndex+1);
                System.out.println("loading custom --host1 == "+host1);
            }
            if(argList.contains("--host2")){
                int argIndex = argList.indexOf("--host2");
                host2 = argList.get(argIndex+1);
                System.out.println("loading custom --host2 == "+host2);
            }
            if(argList.contains("--port")){
                int argIndex = argList.indexOf("--port");
                port = Integer.parseInt(argList.get(argIndex+1));
                System.out.println("loading custom --port == "+port);
            }
            if(argList.contains("--maxconnections")){
                int argIndex = argList.indexOf("--maxconnections");
                maxConnections = Integer.parseInt(argList.get(argIndex+1));
                System.out.println("loading custom --maxconnections == "+maxConnections);
            }
            if(argList.contains("--querycountperthread")){
                int argIndex = argList.indexOf("--querycountperthread");
                queryCountPerThread = Integer.parseInt(argList.get(argIndex+1));
                System.out.println("loading custom --querycountperthread == "+queryCountPerThread);
            }
            if(argList.contains("--pausebetweenthreads")){
                int argIndex = argList.indexOf("--pausebetweenthreads");
                pauseBetweenThreads = Integer.parseInt(argList.get(argIndex+1));
                System.out.println("loading custom --pausebetweenthreads == "+pauseBetweenThreads);
            }
            if(argList.contains("--numberofthreads")){
                int argIndex = argList.indexOf("--numberofthreads");
                numberOfThreads = Integer.parseInt(argList.get(argIndex+1));
                System.out.println("loading custom --numberofthreads == "+numberOfThreads);
            }
            if(argList.contains("--limitsize")){
                int argIndex = argList.indexOf("--limitsize");
                limitSize = Integer.parseInt(argList.get(argIndex+1));
                System.out.println("loading custom --limitsize == "+limitSize);
            }
            if(argList.contains("--username")){
                int argIndex = argList.indexOf("--username");
                userName = argList.get(argIndex+1);
                System.out.println("loading custom --username == "+userName);
            }
            if(argList.contains("--password")){
                int argIndex = argList.indexOf("--password");
                password = argList.get(argIndex + 1);
                System.out.println("loading custom --password == "+password);
            }
            if(argList.contains("--simplereturnfieldspropfilename")){
                int argIndex = argList.indexOf("--simplereturnfieldspropfilename");
                simpleReturnFieldsPropFileName = argList.get(argIndex + 1);
                System.out.println("loading custom --simplereturnfieldspropfilename == "+simpleReturnFieldsPropFileName);
            }
            if(argList.contains("--aliasedreturnfieldspropfilename")){
                int argIndex = argList.indexOf("--aliasedreturnfieldspropfilename");
                aliasedReturnFieldsPropFileName = argList.get(argIndex + 1);
                System.out.println("loading custom --aliasedreturnfieldspropfilename == "+aliasedReturnFieldsPropFileName);
            }
            if(argList.contains("--querystringspropfilename")){
                int argIndex = argList.indexOf("--querystringspropfilename");
                queryStringsPropFileName = argList.get(argIndex + 1);
                System.out.println("loading custom --querystringspropfilename == "+queryStringsPropFileName);
            }
            if (argList.contains("--usessl")) {
                int argIndex = argList.indexOf("--usessl");
                useSSL = Boolean.parseBoolean(argList.get(argIndex + 1));
                System.out.println("loading custom --usessl == " + useSSL);
            }
            if (argList.contains("--cacertpath")) {
                int argIndex = argList.indexOf("--cacertpath");
                caCertPath = argList.get(argIndex + 1);
                System.out.println("loading custom --cacertpath == " + caCertPath);
            }
            if (argList.contains("--cacertpassword")) {
                int argIndex = argList.indexOf("--cacertpassword");
                caCertPassword = argList.get(argIndex + 1);
                System.out.println("loading custom --cacertpassword == " + caCertPassword);
            }
            if (argList.contains("--usercertpath")) {
                int argIndex = argList.indexOf("--usercertpath");
                userCertPath = argList.get(argIndex + 1);
                System.out.println("loading custom --usercertpath == " + userCertPath);
            }
            if (argList.contains("--usercertpass")) {
                int argIndex = argList.indexOf("--usercertpass");
                userCertPassword = argList.get(argIndex + 1);
                System.out.println("loading custom --usercertpass == " + userCertPassword);
            }
        }
        //now that we have the (possibly) new properties files assigned to their variables...
        searchQueries = loadSearchQueries();
        JedisConnectionHelperSettings settings = new JedisConnectionHelperSettings();
        settings.setRedisHost(host);
        settings.setRedisPort(port);
        settings.setUserName(userName);
        if(password!="") {
            settings.setPassword(password);
            settings.setUsePassword(true);
        }
        settings.setMaxConnections(maxConnections); // these will be healthy, tested connections or idle and removed
        settings.setTestOnBorrow(true);
        settings.setConnectionTimeoutMillis(120000);
        settings.setNumberOfMinutesForWaitDuration(1);
        settings.setNumTestsPerEvictionRun(10);
        settings.setPoolMaxIdle(1); //this means less stale connections
        settings.setPoolMinIdle(0);
        settings.setRequestTimeoutMillis(12000);
        settings.setTestOnReturn(false); // if idle, they will be mostly removed anyway
        settings.setTestOnCreate(true);
        if(useSSL){
            settings.setUseSSL(true);
            settings.setCaCertPath(caCertPath);
            settings.setCaCertPassword(caCertPassword);
            settings.setUserCertPath(userCertPath);
            settings.setUserCertPassword(userCertPassword);
        }
        try{
            connectionHelper = new com.redislabs.sa.ot.util.JedisConnectionHelper(settings); // only use a single connection based on the hostname (not ipaddress) if possible
        }catch(Throwable t){
            t.printStackTrace();
            try{
                Thread.sleep(4000);
            }catch(InterruptedException ie){}
            // give it another go - in case the first attempt was just unlucky:
            connectionHelper = new com.redislabs.sa.ot.util.JedisConnectionHelper(settings); // only use a single connection based on the hostname (not ipaddress) if possible
        }
        //Have to do this before the test kicks off!
        JedisPooled jedis = connectionHelper.getPooledJedis();
        jedis.set(PERFORMANCE_TEST_THREAD_COUNTER,"0");
        System.out.println("PERFORMANCE_TEST_THREAD_COUNTER SET TO: "+jedis.get(PERFORMANCE_TEST_THREAD_COUNTER));
        jedis.del(ALL_RESULTS_SORTED_SET);

        //99% of the time you only want a single target redis uri
        URI uri1 =  JedisConnectionHelper.buildURI(host1,port,userName,password);
        URI uri2 =  JedisConnectionHelper.buildURI(host2,port,userName,password);

        URI choice = null;
        //99% of the time you only want a single target redis uri
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
            test.setJedisConnectionBootstrapper(settings);
            test.setConnectionHelper(connectionHelper);
            test.setNumberOfResultsLimit(limitSize);
            test.setTimesToQuery(queryCountPerThread);
            test.setMillisecondPauseBetweenQueryExecutions((pauseBetweenThreads*2)+(limitSize/10)); // this seems reasonable to me as clients getting large results back will take more time to process them before issuing new queries- adjust if you need to
            test.setSearchQueries(searchQueries);
            test.init(); // get jedis connection for the thread and specify query params
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
        waitForCompletion(false,settings,testers.size());

        for(SearchTest test:testers){
            Pipeline jedisPipeline = connectionHelper.getPipeline();
            ArrayList<AtomicLong> numericResults = test.getPerfTestNumericResults();
            ArrayList<String> stringResults = test.getPerfTestResults();
            String threadId = "Thread "+stringResults.get(0).split(":")[0];
            long totalMilliseconds =0l;
            long avgDuration = 0l;
            int resultsCounter =0;
            for(AtomicLong time:numericResults){
                totalMilliseconds+=time.get();
                jedisPipeline.zadd(ALL_RESULTS_SORTED_SET,time.get(),"Thread "+stringResults.get(resultsCounter));
                resultsCounter++;
            }
            jedisPipeline.sync();
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
        System.out.println("\n\t"+jedis.get("throughputEstimateStatement"));
        System.out.println("\nPlease check the --> slowlog <-- on your Redis database to determine if any slowness is serverside or driven by client or network limits\n\n");
    }

    static void waitForCompletion(boolean userDriven, JedisConnectionHelperSettings bootStrapper, int threadsExpected){
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
            JedisPooled jedis = connectionHelper.getPooledJedis();
            System.out.println("Waiting for results to come in from our threads...   ");
            while(noResultsYet){
                try{
                    Thread.sleep(1000);
                    try {
                        jedis.dbSize();
                    }catch(JedisConnectionException exception) {
                        System.out.println("\nwaitForCompletion() Bad thing Happened: client time (millis) is: " + System.currentTimeMillis() + " " + exception.getMessage());
                        boolean brokenConnection = true;
                        while (brokenConnection) { // loop forever until connectionHelper gives pool with good connection:
                            long dbsize = 0;
                            connectionHelper = new JedisConnectionHelper(bootStrapper);
                            jedis = connectionHelper.getPooledJedis();
                            try {
                                Thread.sleep(10);
                                dbsize = jedis.dbSize();//I don't trust 'ping' - I probably should though
                            } catch (Throwable t) {/*do nothing*/}
                            if (dbsize > 0) {
                                brokenConnection = false;
                            }
                        }
                    }
                }catch(InterruptedException ie){ie.printStackTrace();}
                if(jedis.exists(PERFORMANCE_TEST_THREAD_COUNTER)) {
                    int threadsCompleted = Integer.parseInt(jedis.get(PERFORMANCE_TEST_THREAD_COUNTER));
                    if(threadsCompleted>0) {
                        System.out.println("\nIt's been "+(System.currentTimeMillis()-systemTestStartTime)+" milliseconds since this program launched and RESULTS SO FAR -->>  \n" + threadsCompleted + " threads have completed their processing...");
                    }else{
                        System.out.print(".");
                    }
                    if (threadsExpected <= threadsCompleted) {
                        noResultsYet = false;
                        jedis.set("throughputEstimateStatement","Throughput per second for the executed Search Queries is approximately: "+(threadsExpected*queryCountPerThread)/((System.currentTimeMillis()-systemTestStartTime)/1000));
                        System.out.println("Throughput per second for the executed Search Queries is approximately: "+(threadsExpected*queryCountPerThread)/((System.currentTimeMillis()-systemTestStartTime)/1000));
                        System.out.println("\nThe program will now Tally up the times taken by each query for each Thread and provide a summary.\n\n");
                    }
                }
            }
        }
    }

    /**
     * This method loads the search query parameters as defined in the default file idxa_zew_eventsQueryStrings.properties or
     * if specified in the file pointed to by teh argument:  --querystringspropfilename
     * @return
     */
    static ArrayList<String> loadSearchQueries(){
        Properties p = PropertyFileFetcher.loadProps(Main.queryStringsPropFileName);
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
    JedisConnectionHelperSettings bootStrapper = null;
    JedisConnectionHelper connectionHelper = null;
    ArrayList<String> searchQueries = null;
    ArrayList<String> perfTestResults = new ArrayList<>();
    ArrayList<AtomicLong> perfTestNumericResults = new ArrayList<>();
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

    public void setJedisConnectionBootstrapper(JedisConnectionHelperSettings bootStrapper){
        this.bootStrapper = bootStrapper;
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

    public void setConnectionHelper(JedisConnectionHelper connectionHelper) {
        this.connectionHelper = connectionHelper;
    }

    public void setNumberOfResultsLimit(int numberOfResultsLimit) {
        this.numberOfResultsLimit = numberOfResultsLimit;
    }

    public void setTimesToQuery(int timesToQuery) {
        this.timesToQuery = timesToQuery;
    }

    /**
     * This is where the dynamic loading of result attributes to be extracted from the search queries happens
     * It utilizes the default files idxa_zew_eventsSimpleReturnFields.properties and idxa_zew_eventsAliasedReturnFields.properties
     * unless alternative files are specified using the:
     * --simplereturnfieldspropfilename
     * and
     * --aliasedreturnfieldspropfilename
     * command line arguments
     */
    public void init(){
        pool = connectionHelper.getPooledJedis();
        if(showSearchIndexInfo){
            System.out.println("SEARCH_INDEX_INFO: \n\n"+pool.ftInfo(indexAliasName)+"\n\n");
            showSearchIndexInfo=false; // only show it once across all threads
        }
        if(needToLoadFields){
            Properties simpleFields = PropertyFileFetcher.loadProps(Main.simpleReturnFieldsPropFileName);
            for(String f : simpleFields.stringPropertyNames()){
                fieldsReturned.add(FieldName.of(simpleFields.getProperty(f)));
            }
            Properties aliasedFields = PropertyFileFetcher.loadProps(Main.aliasedReturnFieldsPropFileName);

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
    ArrayList<AtomicLong> getPerfTestNumericResults(){
        return perfTestNumericResults;
    }

    /**
    This method shows some example queries expecting the Zewtopia JSON events dataset
     I leave it here commented out
     to showcase the structure of building such a query programmatically
     It would not normally be part of the execution flow as
     loading dynamic query params is a major feature of this program

    void executeQuery(){
        long startTime = System.currentTimeMillis();
        int queryIndex = (int) (System.currentTimeMillis()%searchQueries.size());
        String query = searchQueries.get(queryIndex);
        SearchResult result = pool.ftSearch(indexAliasName, new Query(query)
                .returnFields(
                        FieldName.of("location"), // only a single value exists in a document
                        FieldName.of("$.times.*.civilian").as("first_event_time"), // only returning 1st time in array due to use of *
                        FieldName.of("$.days").as("days"), // multiple days may be returned
                        FieldName.of("$.responsible-parties.hosts.[*].email").as("contact_email"), // Returning the first email only even though there could be more
                        FieldName.of("$.responsible-parties.hosts.[*].phone").as("contact_phone"), // Returning the first phone only even though there could be more
                        FieldName.of("event_name"), // only a single value exists in a document
                        FieldName.of("$.times[*].military").as("military1"), // only returning 1st time in array due to use of *
                        FieldName.of("$.description")
                ).limit(0,numberOfResultsLimit).dialect(Main.dialectVersion)
        );
        long duration = (System.currentTimeMillis()-startTime);
        perfTestResults.add(testInstanceID+": executed query: "+query+" (with "+result.getTotalResults()+" results and limit size of "+numberOfResultsLimit+") Execution took: "+duration+" milliseconds");
        perfTestNumericResults.add(duration);
    }
     */

    /**
     * This is the method normally used when running this program
     * It leverages the dynamic loading of search parameters which are defined in two possible properties files
     */
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
        SearchResult result = null;
        try {
            result = pool.ftSearch(indexAliasName, new Query(query)
                    .returnFields(returnFieldsArg)
                    .limit(0, numberOfResultsLimit)
                    .dialect(Main.dialectVersion));
        }catch(redis.clients.jedis.exceptions.JedisConnectionException exception) {
            System.out.println("\nexecuteQueryLoadedReturnFields() Bad thing Happened: client time (millis) is: "+System.currentTimeMillis()+" "+exception.getMessage());
            boolean brokenConnection = true;
            while(brokenConnection){ // loop forever until connectionHelper gives pool with good connection:
                long dbsize = 0;
                this.connectionHelper = new JedisConnectionHelper(bootStrapper);
                this.pool  = connectionHelper.getPooledJedis();
                try{
                    Thread.sleep(10);
                    dbsize = pool.dbSize();
                }catch(Throwable t){/*do nothing*/}
                if(dbsize>0){
                    brokenConnection=false;
                }
            }
            result = pool.ftSearch(indexAliasName, new Query(query)
                    .returnFields(returnFieldsArg)
                    .limit(0, numberOfResultsLimit)
                    .dialect(Main.dialectVersion));
        }
        if(showSample) {
            //testing query results:
            System.out.println("queryArgs == "+query);
            System.out.println("returnFieldsArgs[0] == "+returnFieldsArg[0]);
            String output="\nNo Results returned";
            if(result.getTotalResults()>0) {
                output = "sample matching document returned: \n" + result.getDocuments().get(0).getId();

                for (Map.Entry<String, Object> e : result.getDocuments().get(0).getProperties()) {
                    output += "\n" + e.getKey() + " " + e.getValue();
                }
            }
            System.out.println("\n"+result.getTotalResults() + " results matched -- " + output);
            showSample=false;
        }

        long duration = (System.currentTimeMillis()-startTime);
        perfTestResults.add(testInstanceID+": executed query: "+query+" (with "+result.getTotalResults()+" results and limit size of "+numberOfResultsLimit+") Execution took: "+duration+" milliseconds");
        perfTestNumericResults.add(new AtomicLong(duration));
    }

}


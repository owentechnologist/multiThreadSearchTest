package com.redislabs.sa.ot.util;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.*;
import redis.clients.jedis.providers.PooledConnectionProvider;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

public class JedisConnectionHelper {
    final PooledConnectionProvider connectionProvider;
    final JedisPooled jedisPooled;

    /**
     * Used when you want to send a batch of commands to the Redis Server
     * @return Pipeline
     */
    public Pipeline getPipeline(){
        return  new Pipeline(jedisPooled.getPool().getResource());
    }

    /**
     * Assuming use of Jedis 4.3.1:
     * https://github.com/redis/jedis/blob/82f286b4d1441cf15e32cc629c66b5c9caa0f286/src/main/java/redis/clients/jedis/Transaction.java#L22-L23
     * @return Transaction
     */
    public Transaction getTransaction(){
        return new Transaction(jedisPooled.getPool().getResource());
    }

    /**
     * Obtain the default object used to perform Redis commands
     * This code does an extraordinary thing in that it adds a key to Redis
     * This is done to force a healthy connection to be returned
     * This extra key writing
     * should not be necessary and should be eventually cleaned out
     * @return JedisPooled
     */
    public JedisPooled getPooledJedis(){
        long dbsize =0;
        while(dbsize<1) {
            try {
                dbsize = jedisPooled.dbSize();
                if(dbsize<1){
                    jedisPooled.set("com.redislabs.sa.ot.util.JedisConnectionHelper.testKey","ot");
                    jedisPooled.expire("com.redislabs.sa.ot.util.JedisConnectionHelper.testKey",2);
                }
            } catch (Throwable t) {
                t.printStackTrace();
                try{
                    Thread.sleep(10);
                }catch(Throwable tt){}
            }
        }
        return jedisPooled;
    }

    /**
     * Use this to build the URI expected in this classes' Constructor
     * @param host
     * @param port
     * @param username
     * @param password
     * @return
     */
    public static URI buildURI(String host, int port, String username, String password){
        URI uri = null;
        try {
            if (!("".equalsIgnoreCase(password))) {
                uri = new URI("redis://" + username + ":" + password + "@" + host + ":" + port);
            } else {
                uri = new URI("redis://" + host + ":" + port);
            }
        } catch (URISyntaxException use) {
            use.printStackTrace();
            System.exit(1);
        }
        return uri;
    }

    public JedisConnectionHelper(JedisConnectionHelperSettings bs){
        URI uri = buildURI(bs.getRedisHost(), bs.getRedisPort(), bs.getUserName(),bs.getPassword());
        HostAndPort address = new HostAndPort(uri.getHost(), uri.getPort());
        JedisClientConfig clientConfig = null;
        System.out.println("Connection Creation Debug --> "+uri.getAuthority().split(":").length);
        if(uri.getAuthority().split(":").length==3){
            String user = uri.getAuthority().split(":")[0];
            String password = uri.getAuthority().split(":")[1];
            password = password.split("@")[0];
            System.out.println("\n\nUsing user: "+user+" / password @@@@@@@@@@"+password);
            clientConfig = DefaultJedisClientConfig.builder().user(user).password(password)
                    .connectionTimeoutMillis(bs.getConnectionTimeoutMillis()).timeoutMillis(bs.getRequestTimeoutMillis()).build(); // timeout and client settings

        }else {
            clientConfig = DefaultJedisClientConfig.builder()
                    .connectionTimeoutMillis(bs.getConnectionTimeoutMillis()).timeoutMillis(bs.getRequestTimeoutMillis()).build(); // timeout and client settings
        }
        GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxIdle(bs.getPoolMaxIdle());
        poolConfig.setMaxTotal(bs.getMaxConnections());
        poolConfig.setMinIdle(bs.getPoolMinIdle());
        poolConfig.setMaxWait(Duration.ofMinutes(bs.getNumberOfMinutesForWaitDuration()));
        poolConfig.setTestOnCreate(bs.isTestOnCreate());
        poolConfig.setTestOnBorrow(bs.isTestOnBorrow());
        poolConfig.setNumTestsPerEvictionRun(bs.getNumTestsPerEvictionRun());

        this.connectionProvider = new PooledConnectionProvider(new ConnectionFactory(address, clientConfig), poolConfig);
        this.jedisPooled = new JedisPooled(connectionProvider);
    }
}

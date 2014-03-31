package com.crossbusiness.resiliency.demo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.crossbusiness.resiliency.annotation.*;
import org.springframework.scheduling.annotation.AsyncResult;

/**
 * Created by schintha on 2/23/14.
 */
//@Service("tarService")
//@Scope(value="prototype", proxyMode=ScopedProxyMode.TARGET_CLASS)
class TarService{
    //private static final Logger logger = LoggerFactory.getLogger(TarService.class);

    public String greet() throws IOException {
        if (new Random().nextInt(2) == 1) throw new NullPointerException("dummy exception in...greet");
        return "Hello from Tar in greet ";
    }

    public String salute() throws FileNotFoundException {
        if (new Random().nextInt(2) == 1) throw new NullPointerException("dummy exception in...salute");
        return "Hello from Tar in salute ";
    }

    @Async("sumoExecutor")
    Future<String> doSomething(String arg1) {
        //logger.debug ("beginning work ... ");
        int loop = 0;
        try{
            //do batch jobs incrementally
            while(!Thread.currentThread().isInterrupted() & loop < 6){
                //do a sub-task of long running batch job here
                TimeUnit.SECONDS.sleep(1);  // wait for one seconds
                loop++	;
                //logger.debug ("loop count ... ${}",loop);
            }
        }catch(InterruptedException ie){
            //logger.debug ("got Interrupted!... loop count ... {}, Cleaning the resource...",loop);
            //do cleanup job... close connections etc...
            Thread.currentThread().interrupt();
        }
        //logger.debug ("loop count Out ... ${}",loop);
        return new AsyncResult<String>("REPLY: ${arg1} looped: {loop} times");
    }
}

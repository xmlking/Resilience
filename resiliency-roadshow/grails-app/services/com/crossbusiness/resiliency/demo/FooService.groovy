package com.crossbusiness.resiliency.demo

import com.crossbusiness.resiliency.annotation.Fallback;
import grails.transaction.Transactional

@Transactional(readOnly = true)
@Fallback(['barService','jarService'])
class FooService implements Greet{

    static scope = "prototype"

    @Override
    public String greet() throws IOException{
        if (new Random().nextInt(5) == 4) throw new IOException("dummy exception from FooService in...greet");
        return "Hello from Foo in greet ";
    }

    @Override
    public String salute() throws FileNotFoundException{
        if (new Random().nextInt(5) == 4) throw new FileNotFoundException("dummy exception from FooService in...salute");
        return "Hello from Foo in salute ";
    }
}

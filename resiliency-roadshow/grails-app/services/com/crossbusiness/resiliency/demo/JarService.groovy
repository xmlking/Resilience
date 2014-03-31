package com.crossbusiness.resiliency.demo

import grails.transaction.Transactional

@Transactional(readOnly = true)
class JarService implements Greet{

    static scope = "prototype"

    @Override
    public String greet() throws IOException{
        if (new Random().nextInt(2) == 1) throw new NullPointerException("dummy exception from JarService in...greet");
        return "Hello from Jar in greet ";
    }

    @Override
    public String salute() throws FileNotFoundException{
        if (new Random().nextInt(2) == 1) throw new NullPointerException("dummy exception from JarService in...salute");
        return "Hello from Jar in salute ";
    }
}

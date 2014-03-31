package com.crossbusiness.resiliency.demo

import grails.transaction.Transactional

@Transactional(readOnly = true)
class BarService implements Greet{

    static scope = "prototype"

    @Override
    public String greet() throws IOException{
        if (new Random().nextInt(2) == 1) throw new IOException("dummy exception from BarService in...greet");
        return "Hello from Bar in greet";
    }

    @Override
    public String salute() throws FileNotFoundException{
        if (new Random().nextInt(2) == 1) throw new FileNotFoundException("dummy exception from BarService in...salute");
        return "Hello from Bar in salute";
    }
}
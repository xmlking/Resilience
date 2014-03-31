package com.crossbusiness.resiliency.demo
import com.crossbusiness.resiliency.annotation.Fallback;
/**
 * Created by schintha on 2/23/14.
 */

//@Alternatives(['fooService','barService','jarService'])
public interface Greet {
    public String greet() throws IOException;
    public String salute() throws FileNotFoundException;
}
package com.umidity.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AsyncCallerTest {
    AsyncCaller caller;

    @AfterEach
    void tearDown(){
        caller = null;
    }

    @Test
    void testByCityName(){
        caller = new AsyncCaller( new ApiCaller("a8f213a93e1af4abd8aa6ea20941cb9b", EMode.JSON, EUnits.Metric),
                AsyncCaller.AsyncMethod.byCityName, 1000, "Senigallia", "","");
        try {
            assertNull(caller.apiResponse);
            caller.start();
            Thread.sleep(2000);
            assertEquals("Senigallia", caller.apiResponse.name);
            caller.close();
            Thread.sleep(2000);
            caller.clearResponse();
            assertNull(caller.apiResponse);
        } catch (InterruptedException e) {
            fail();
        }
    }

}
package com.umidity.api.caller;


import com.umidity.api.response.ApiResponse;
import com.umidity.api.response.ForecastResponse;
import com.umidity.api.response.OneCallResponse;

import java.io.IOException;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.Vector;

public class AsyncCaller extends Thread {
    public enum AsyncMethod{

        /**
         * Arguments: float lat, float lon, EnumSet< EExclude > excludes.
         */
        oneCall1,
        /**
         * Arguments: float lat, float lon,long dt,  EnumSet< EExclude > excludes.
         */
        oneCall2,
        /**
         * Arguments: String name, String zip (default = ""), String countryCode (default = "").
         */
        byCityName,
        /**
         * Arguments: int[] ids.
         */
        byCityId,
        /**
         * Arguments: float lat, float lon.
         */
        byCoordinates,
        /**
         * Arguments: String zipCode, String countryCode (default = "").
         */
        byZipCode,
        /**
         * Arguments: String name, String zip (default = ""), String countryCode (default = "").
         */
        forecastByCityName,
        /**
         * Arguments: int[] ids.
         */
        forecastByCityId,
        /**
         * Arguments: String zipCode, String countryCode (default = "").
         */
        forecastByCoordinates,
        /**
         * Arguments: String name, String zip (default = ""), String countryCode (default = "").
         */
        forecastByZipCode,
    }

    /**
     * Id true, the thread will stop.
     */
    private       boolean                    close;
    /**
     * Indicates whether the thread is running or not.
     */
    private       boolean                    isRunning;
    /**
     * If set to true, after <em>myAsyncCaller.Start()</em> is launched, the thread will run just one time and than it stops.
     */
    private       boolean                    oneTime;
    /**
     * A vector of oneCallResponses
     */
    public        Vector< OneCallResponse >  oneCallResponse;
    /**
     * A vector of ApiResponses
     */
    public        Vector< ApiResponse >      apiResponse;
    /**
     * A vector of ForecastResponses
     */
    public        Vector< ForecastResponse > forecastResponse;
    /**
     * The api caller used for each api call.
     */
    private final ApiCaller                  caller;
    /**
     * the selected method for this AsyncCaller
     */
    private final AsyncMethod method;
    /**
     * Time (in milliseconds) to wait until next call
     */
    private final long        timeToWait;
    /**
     * Array of arguments. The needed arguments are dependent on which AsyncMethod is set.
     */
    private final Object[]    args;

    public boolean getRunningStatus(){return isRunning; }
    public void clearResponse(){
        oneCallResponse.clear();
        apiResponse.clear();
        forecastResponse.clear();
    }

    /**
     * @param caller The api caller used for each api call.
     * @param timeToWait Time (in milliseconds) to wait between calls.
     * @param method the selected method for this AsyncCaller.
     * @param args arguments needed for that method.
     */
    public AsyncCaller(ApiCaller caller, long timeToWait, AsyncMethod method, Object... args) {
        this.caller = caller;
        this.method = method;
        this.timeToWait = timeToWait;
        this.args = args;
        oneTime = false;
        isRunning = false;
    }

    /**
     * Adds a listener to the listeners
     * @param listener
     */
    public void addListener(ApiListener listener){caller.addListener(listener);}

    /**
     * Stop the thread.
     */
    public void close(){ close = true; }

    /**
     * After a call is made, the thread will stop
     * @param value
     */
    public void setOneTime(boolean value){ oneTime = value; }

    public void run() {
        long lastExecution = 0;
        try {
            while(!close){
                isRunning = true;
                clearResponse();
                long now = Calendar.getInstance().getTimeInMillis();
                if(now - lastExecution >= timeToWait){
                    lastExecution = now;
                    switch (method){
                        case oneCall1 ->
                                oneCallResponse.add(caller.oneCall((float)args[0], (float)args[1], (EnumSet<EExclude>)args[2]));
                        case oneCall2 ->
                                oneCallResponse.add(caller.oneCall((float)args[0],
                                        (float)args[1], (long)args[2], (EnumSet<EExclude>) args[3]));
                        case byCityName ->
                                apiResponse.add(caller.getByCityName((String)args[0],
                                        (String)args[1], (String)args[2]));
                        case byCoordinates ->
                                apiResponse.add(caller.getByCoordinates((float)args[0],
                                        (float)args[1]));
                        case byCityId -> getByCityId((int[])args[0]);
                        case byZipCode ->
                                apiResponse.add(caller.getByZipCode((String)args[0], (String)args[1]));
                        case forecastByCityId ->
                                forecastResponse.add(caller.getForecastByCityId((String)args[0]));
                        case forecastByZipCode ->
                                forecastResponse.add(caller.getForecastByZipCode((String)args[0],
                                        (String)args[1]));
                        case forecastByCityName ->
                                forecastResponse.add(caller.getForecastByCityName((String)args[0],
                                        (String) args[1],(String)args[1]));
                        case forecastByCoordinates ->
                                forecastResponse.add(caller.getForecastByCoordinates((float)args[0],
                                        (float)args[1]));
                        }
                        if(oneTime)return;
                }
            }
            } catch (IOException e) {
            e.printStackTrace();
        }finally {
            close = false;
            isRunning = false;
        }
    }

    /**
     * Makes call to "By City Id" api. If there are more than one argument, it will make a call for each argument.
     * @param args a list of city ids (a list of Strings)
     * @throws IOException
     */
    private void getByCityId(int[] args) throws IOException {
        apiResponse.clear();
        for(Object o:args){
            apiResponse.add(caller.getByCityId(String.valueOf(o)));
        }
    }
}

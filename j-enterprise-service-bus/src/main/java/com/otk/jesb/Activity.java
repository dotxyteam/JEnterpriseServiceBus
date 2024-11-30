package com.otk.jesb;
public interface Activity {
    Result execute() throws Exception;

    public static interface Result {
    }
}
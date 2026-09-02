package com.example;

import junit.framework.Test;
import junit.framework.TestResult;

public class JUnit3CustomTest implements Test {

    @Override
    public void run(final TestResult result) {
        result.startTest(this);
        result.endTest(this);
    }

    @Override
    public int countTestCases() {
        return 1;
    }


    @Override
    public String toString() {
        return "customTest";
    }
}

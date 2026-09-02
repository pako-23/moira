package com.example;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.HashMap;

public class TestAppRegistry {
    private static final Map<String, String[]> tests;
    private static final Map<String, Boolean> passing;

    static {
        tests = new HashMap<>();
        passing = new HashMap<>();

        registerTestCasesShortDescriptionEntry(com.example.JUnit4ExampleTest.class,
            new Entry("testSimplePassing", true),
            new Entry("testSimpleFailing", false),
            new Entry("testIgnored", true));

        registerTestCasesShortDescriptionEntry(
            com.example.JUnit4SubclassTest.class,
            new Entry("testPassing", true),
            new Entry("testFailing", false),
            new Entry("testAbstractMethod", true),
            new Entry("testSubclassPassingTest", true),
            new Entry("testSubclassFailingTest", false));

        registerTestCasesShortDescriptionEntry(
            com.example.JUnit3ExampleTest.class,
            new Entry("testSimplePassing", true),
            new Entry("testSimpleFailing", false));

        registerTestCasesShortDescriptionEntry(
            com.example.SingleTestCaseSuiteMethod.class,
            new Entry("testPassing", true));

        registerTestCasesShortDescriptionEntry(
            com.example.PrintingTest.class,
            new Entry("testPrinting", true));


       registerTestCasesFullDescriptionEntry(
           com.example.JUnit3SuiteTestAll.class,
           new Entry(String.format("[testFailing(%s)]", com.example.JUnit3FirstChildSimpleTest.class.getName()), false),
          new Entry(
                  String.format(
                      "[testPassing(%s)]", com.example.JUnit3FirstChildSimpleTest.class.getName()), true),
          new Entry(
              String.format("[testParameter(%s)]#0", com.example.JUnit3ParametrizedTest.class.getName()),
              true),
           new Entry(
              String.format("[testParameter(%s)]#1", com.example.JUnit3ParametrizedTest.class.getName()),
               false),
           new Entry(
              String.format("[testParameter(%s)]#2", com.example.JUnit3ParametrizedTest.class.getName()),
               false));
           

        registerTestCasesFullDescriptionEntry(
            com.example.DescribableTestSuite.class,
            new Entry("[customDescription(com.example.DescribableTestCase)]", true),
            new Entry("[customTest(com.example.JUnit3CustomTest)]", true));
    }


    public static String[] getTestCases(final Class<?> testClass) {
        return getTestCases(testClass.getName());
    }

    public static String[] getTestCases(final String testClass) {
        return tests.getOrDefault(testClass, new String[0]);
    }

    public static boolean isTestCasePassing(final String testCase) {
        return passing.get(testCase);
    }


    private static void registerTestCasesShortDescriptionEntry(final Class<?> testClass, final Entry... testCases) {
        final Function<Entry, String> mapper = entry -> String.format("%s[%s(%s)]", testClass.getName(), entry.description, testClass.getName());
        
        tests.put(testClass.getName(), Stream.of(testCases)
            .map(mapper)
            .toArray(String[]::new));

        Stream.of(testCases).forEach(entry -> passing.put(mapper.apply(entry), entry.passing ));
    }

    private static void registerTestCasesFullDescriptionEntry(final Class<?> testClass, final Entry... testCases) {
        final Function<Entry, String> mapper = entry ->  testClass.getName() + entry.description;


        tests.put(testClass.getName(), Stream.of(testCases)
            .map(mapper)
            .toArray(String[]::new));

        Stream.of(testCases).forEach(entry -> passing.put(mapper.apply(entry), entry.passing ));
    }

    private static final class Entry {
        private final String description;
        private final boolean passing;

        public Entry(final String description, final boolean passing) {
            this.description = description;
            this.passing = passing;
        }
    }
}

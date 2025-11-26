package ch.usi.inf.runner;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.AbstractList;
import java.util.ArrayList;

public class TestsFinder {
  private final ArrayList<String> tests;
  private final ClassLoader classLoader;
  private final String prefix;

  public TestsFinder(final File entry) {
    URL[] urls;
    tests = new ArrayList<>();

    try {
      urls = new URL[] {entry.toURI().toURL()};
    } catch (MalformedURLException e) {
      urls = new URL[0];
    }

    classLoader = new URLClassLoader(urls, TestsFinder.class.getClassLoader());
    prefix = entry.getPath() + File.separator;

    findTests(entry);
  }

  private void findTests(final File entry) {
    if (!entry.isDirectory()) {
      findTestsFromFile(entry);
      return;
    }

    File[] files = entry.listFiles();
    for (final File file : files) findTests(file);
  }

  private void findTestsFromFile(final File file) {
    if (!file.isFile() || !file.getName().endsWith(".class")) {
      return;
    }

    String className =
        file.getPath().replaceFirst(prefix, "").replace(".class", "").replace(File.separator, ".");

    try {
      Class<?> clazz = classLoader.loadClass(className);

      for (final Method method : clazz.getMethods())
        for (final Annotation annotation : method.getAnnotations())
          if (annotation.annotationType().getName().equals("org.junit.Test")) {
            tests.add(clazz.getName() + "#" + method.getName());
            break;
          }
    } catch (Exception e) {
      return;
    }
  }

  public AbstractList<String> getTests() {
    return tests;
  }
}

package ch.usi.inf.runner;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TestsFinder {
  private final Set<String> tests;
  private final ClassLoader classLoader;
  private final String prefix;
  private final boolean classOnly;

  public TestsFinder(final File entry, final boolean classOnly) {
    URL[] urls;
    tests = new HashSet<>();
    this.classOnly = classOnly;

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

    final String className =
        file.getPath().replaceFirst(prefix, "").replace(".class", "").replace(File.separator, ".");

    try {
      final Class<?> clazz = classLoader.loadClass(className);

      findJUnit4Tests(clazz);
    } catch (Exception e) {
      return;
    }
  }

  private void findJUnit4Tests(final Class<?> clazz) {
    if (Modifier.isAbstract(clazz.getModifiers())) return;

    for (final Method method : clazz.getMethods())
      for (final Annotation annotation : method.getAnnotations()) {
        final boolean isTestMethod = annotation.annotationType().getName().equals("org.junit.Test");
        if (!isTestMethod) continue;

        if (classOnly) {
          tests.add(clazz.getName());
          return;
        } else {
          tests.add(clazz.getName() + "#" + method.getName());
          break;
        }
      }
  }

  public Collection<String> getTests() {
    return tests;
  }
}

/*
 * Copyright (C) 2021 Google, LLC.
 * SPDX-License-Identifier: BSD-3-Clause
 */
package de.jflex.migration.unicodedatatest.base;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static de.jflex.migration.util.JavaResources.readResource;

import com.google.common.collect.ImmutableList;
import de.jflex.util.javac.JavaPackageUtils;
import de.jflex.velocity.Velocity;
import de.jflex.version.Version;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.velocity.runtime.parser.ParseException;

public abstract class AbstractGenerator<T extends UnicodeVersionTemplateVars> {

  // TODO(regisd) Add This in UnicodeProperties
  private static final ImmutableList<Version> KNOWN_VERSIONS =
      ImmutableList.of(
          new Version(1, 1),
          new Version(2, 0),
          new Version(2, 1),
          new Version(3, 0),
          new Version(3, 1),
          new Version(3, 2),
          new Version(4, 0),
          new Version(4, 1),
          new Version(5, 0),
          new Version(5, 1),
          new Version(5, 2),
          new Version(6, 0),
          new Version(6, 1),
          new Version(6, 2),
          new Version(6, 3),
          new Version(7, 0),
          new Version(8, 0),
          new Version(9, 0),
          new Version(10, 0),
          new Version(11, 0),
          new Version(12, 0),
          new Version(12, 1));

  protected final String templateName;
  protected final UnicodeVersion unicodeVersion;

  protected AbstractGenerator(String templateName, UnicodeVersion unicodeVersion) {
    if (templateName.endsWith(".vm")) {
      this.templateName = templateName.substring(0, templateName.length() - 3);
    } else {
      this.templateName = templateName;
    }
    this.unicodeVersion = unicodeVersion;
  }

  public static ImmutableList<Version> olderAges(Version version) {
    return KNOWN_VERSIONS.stream()
        .filter(v -> Version.EXACT_VERSION_COMPARATOR.compare(v, version) <= 0)
        .collect(toImmutableList());
  }

  /** Returns the generated file. */
  public Path generate(Path outDir) throws IOException, ParseException {
    T vars = createTemplateVars();
    vars.updateFrom(unicodeVersion);
    vars.templateName = templateName;

    Path javaPackageOutDir =
        outDir.resolve("javatests").resolve(unicodeVersion.javaPackageDirectory());
    Files.createDirectories(javaPackageOutDir);
    Path outFile = javaPackageOutDir.resolve(getOuputFileName(vars));
    InputStreamReader templateReader;
    try {
      templateReader = readResourceTemplate();
    } catch (NullPointerException e) {
      throw new IllegalArgumentException(
          "Could not read template in java resources for template", e);
    }
    try {
      Velocity.render(templateReader, templateName, vars, outFile.toFile());
    } catch (Exception e) {
      throw new RuntimeException("Error rendering '" + templateName + "' with " + vars, e);
    }
    return outFile;
  }

  /** Reads the template from base or from the test package. */
  private InputStreamReader readResourceTemplate() {
    try {
      return readResource(
          Paths.get(JavaPackageUtils.getPathForClass(AbstractGenerator.class))
              .resolve(templateName + ".vm")
              .toString());
    } catch (NullPointerException e) {
      return readResource(
          Paths.get(JavaPackageUtils.getPathForClass(this.getClass()))
              .resolve(templateName + ".vm")
              .toString());
    }
  }

  protected abstract T createTemplateVars();

  protected abstract String getOuputFileName(T vars);
}

package com.io7m.callisto.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class LocaleTest
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(LocaleTest.class);
  }

  private LocaleTest()
  {

  }

  public static void main(
    final String[] args)
  {
    final List<Locale> available = Arrays.asList(Locale.getAvailableLocales());
    available.sort(Comparator.comparing(Locale::toString));

    for (final Locale locale : available) {
      LOG.debug("locale: {}", locale.toLanguageTag());
    }

    LOG.debug("locale current: {}", Locale.getDefault());
  }
}

package gnu.localegen;

import gnu.ldml.Analyzer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.IOException;

public class Main
{

  static public void main(String args[]) throws Exception
  {
    printVersion();
    if (args.length == 0)
      {
        System.out.println("Invalid number of arguments.");
        printUsage();
        return;
      }
    Map localeAnalyzers = new HashMap();
    Map collationAnalyzers = new HashMap();
    for (int i = 0; i < args.length; i++)
      {
        URL u;
        Analyzer a;
        try
          {
            try
              {
                u = new URL(args[i]);
              }
            catch (MalformedURLException e)
              {
                u = new URL("file:" + args[i]);
              }
            System.out.println("Parsing/Analyzing initial URL " + u);
            a = new Analyzer(u);
          }
        catch (IOException e)
          {
            System.out
              .println("It is impossible to grab the requested file (reason="
                       + e.getMessage() + ")");
            e.printStackTrace();
            System.out.println("Exiting.");
            return;
          }
        a.flattenTree();
        Collection locales = a.getLocales();
        for (Iterator j = locales.iterator(); j.hasNext();)
          {
            String locale = (String) j.next();
            if (a.isCollation())
              {
                collationAnalyzers.put(locale, a);
              }
            else
              {
                localeAnalyzers.put(locale, a);
              }
          }
      }
    for (Iterator i = localeAnalyzers.keySet().iterator(); i.hasNext();)
      {
        String locale = (String) i.next();
        Analyzer a = (Analyzer) localeAnalyzers.get(locale);
        Analyzer ca = (Analyzer) collationAnalyzers.get(locale);
        List analyzers = (ca == null) ? Collections.singletonList(a) : Arrays
          .asList(new Analyzer[] { a, ca });
        System.out.println("Generating Java source code for " + locale
                           + " in gnu.java.locale");
        JavaGenerator generator = new JavaGenerator("gnu.java.locale",
                                                    analyzers, locale);
        generator.generate(null);
      }
  }

  static void printUsage()
  {
    System.out.println(" Usage: [URLs]");
    System.out.println();
    System.out
      .println("The generator takes only URL on command line. The corresponding files will be retrieved and parsed.");
    System.out
      .println("The Java source code for GNU Classpath is generated in gnu/java/locale/LocaleInformation_[name]");
    System.out
      .println("where \"name\" is the proper name of the original XML file (atm), i.e. the base file name.");
    System.exit(1);
  }
  static void printVersion()
  {
    System.out.println(" This is the LDML to GNU Classpath converter");
    System.out.println("   Copyright (C) 2004 The Free Software Foundation.");
    System.out.println();
  }
}
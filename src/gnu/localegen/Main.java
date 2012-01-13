/*/
 * gnu.localegen.Main Copyright (C) 2004, 2008 Free Software Foundation,
 * Inc.
 *
 * This file is part of GNU Classpath.
 *
 * GNU Classpath is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 *
 * GNU Classpath is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GNU Classpath; see the file COPYING. If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
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
    Map<String,Analyzer> localeAnalyzers = new HashMap<String,Analyzer>();
    Map<String,Analyzer> collationAnalyzers = new HashMap<String,Analyzer>();
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
        Collection<String> locales = a.getLocales();
        for (Iterator<String> j = locales.iterator(); j.hasNext();)
          {
            String locale = j.next();
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
    for (Iterator<String> i = localeAnalyzers.keySet().iterator(); i.hasNext();)
      {
        String locale = i.next();
        Analyzer a = localeAnalyzers.get(locale);
        Analyzer ca = collationAnalyzers.get(locale);
        List<Analyzer> analyzers = (ca == null) ? Collections.singletonList(a) : Arrays
          .asList(new Analyzer[] { a, ca });
        System.out.println("Generating Java source code for " + locale
                           + " in gnu.java.locale");
        PropertiesGenerator generator = new PropertiesGenerator("gnu.java.locale",
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
    System.out.println("   Copyright (C) 2004, 2006, 2008 The Free Software Foundation.");
    System.out.println();
  }
}

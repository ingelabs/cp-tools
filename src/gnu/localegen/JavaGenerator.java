/*
 * gnu.localegen.JavaGenerator Copyright (C) 2004, 2005
 * Free Software Foundation, Inc.
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
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Hashtable;
import gnu.ldml.Element;
import gnu.ldml.DataElement;
import gnu.ldml.ListDataElement;
import gnu.ldml.OrderedListElement;

public class JavaGenerator
{
  private static final int JAVA_HEXLENGTH = 4;
  private final Collection<Analyzer> analyzers;
  private final String locale;
  private final String inPackage;
  private ArrayList<JavaContent> localeContents = new ArrayList<JavaContent>();
  private static final String[] classpathDateFormats = { "shortDateFormat",
                                                        "mediumDateFormat",
                                                        "longDateFormat",
                                                        "fullDateFormat" };
  private static final String[] classpathTimeFormats = { "shortTimeFormat",
                                                        "mediumTimeFormat",
                                                        "longTimeFormat",
                                                        "fullTimeFormat" };
  private static final String[] classpathZoneOrder = { "zone.long.standard",
                                                      "zone.short.standard",
                                                      "zone.long.daylight",
                                                      "zone.short.daylight" };
  private static final String collatorIdentifiers = "<=,;@&!";

  /*
   * Interface for locale string generation in GNU Classpath.
   */
  interface JavaContent
  {
    String getName();

    String getData();

    boolean isPackage();

    void generateContent(PrintWriter o);

    boolean isUsable();
  }

  /*
   * Convert a UTF string in a java source compatible string.
   */
  static String convertToJavaString(String s)
  {
    if (s == null)
      return "null";
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < s.length(); i++)
      {
        char c = s.charAt(i);
        // Transform non-ASCII character into an escaped unicode character.
        if (c > 127)
          {
            buf.append("\\u");
            String hexString = Integer.toHexString((int) c);
            for (int j = 0; j < JAVA_HEXLENGTH - hexString.length(); j++)
              buf.append('0');
            buf.append(hexString);
          }
        else if (c == '"')
          {
            buf.append("\\\"");
          }
        else if (c == '\\')
          {
            buf.append("\\\\");
          }
        else
          buf.append(c);
      }
    return buf.toString();
  }

  /*
   * This class implements JavaContent for simple string locale data. The entry
   * has the name "name" and contains the data specified by "data".
   */
  class StringContent implements JavaContent
  {
    private String name;
    private String data;

    public StringContent(String name, String data)
    {
      this.name = name;
      this.data = data;
    }

    public boolean isPackage()
    {
      return false;
    }

    public String getName()
    {
      return name;
    }

    public String getData()
    {
      return data;
    }

    public void generateContent(PrintWriter o)
    {
    }

    public boolean isUsable()
    {
      return true;
    }
  }

  /*
   * This class implements JavaContent for big string locale data. The entry has
   * the name "name" and contains the data specified by "data".
   */
  class BigStringContent implements JavaContent
  {
    private String name;
    private String data;

    public BigStringContent(String name, String data)
    {
      this.name = name;
      this.data = convertToJavaString(data);
    }

    public boolean isPackage()
    {
      return true;
    }

    public String getName()
    {
      return name;
    }

    public String getData()
    {
      return null;
    }

    public void generateContent(PrintWriter o)
    {
      int pos = 0;
      int charOnLine = 0;
      o.println("  private static final String " + name + " = ");
      o.print("\t\"");
      while (pos < data.length())
        {
          if (charOnLine >= 60)
            {
              o.println("\" +");
              o.print("\t\"");
              charOnLine = 0;
            }
          if (data.charAt(pos) == '\\' && charOnLine >= 54)
            {
              charOnLine = 60;
              continue;
            }
          o.print(data.charAt(pos));
          pos++;
          charOnLine++;
        }
      o.println("\";");
    }

    public boolean isUsable()
    {
      return true;
    }
  }

  /*
   * This class implements JavaContent for simple ordered list. The lists are
   * ordered according to their index in the array. The class builds an external
   * array of Object to store the list and use the identifier name for both the
   * locale id name and the java object name.
   */
  class ManualListContent implements JavaContent
  {
    private String name;
    private Object[] data;

    public ManualListContent(String name, Object[] data)
    {
      this.name = name;
      this.data = data;
    }

    public boolean isPackage()
    {
      return true;
    }

    public String getName()
    {
      return name;
    }

    public String getData()
    {
      return null;
    }

    public void generateContent(PrintWriter o)
    {
      o.println("  private static final String[] " + name + " = {");
      for (int i = 0; i < data.length; i++)
        {
          o.print("    \"" + convertToJavaString(data[i].toString()) + "\"");
          o.println(",");
        }
      o.println("  };");
    }

    public boolean isUsable()
    {
      return true;
    }
  }

  /*
   * This class implements JavaContent for string ordered list. The constructor
   * accepts a hashtable and an array of strings to specify the order. It can
   * also optionally adds some "null" entries at the beginning of the array with
   * addNull.
   */
  class OrderedListContent implements JavaContent
  {
    private String name;
    private Hashtable<String,Object> data;
    private String[] order;
    private int prependNull;
    private int appendNull;

    public OrderedListContent(String name, Hashtable<String,Object> data, String[] order,
                              int prependNull, int appendNull)
    {
      this.name = name;
      this.data = data;
      this.order = order;
      this.prependNull = prependNull;
      this.appendNull = appendNull;
    }

    public boolean isPackage()
    {
      return true;
    }

    public String getName()
    {
      return name;
    }

    public String getData()
    {
      return null;
    }

    public void generateContent(PrintWriter o)
    {
      o.println("  private static final String[] " + name + " = {");
      for (int i = 0; i < prependNull; i++)
        o.println("    null,");
      for (int i = 0; i < order.length; i++)
        {
          Object contentElement = data.get(order[i]);
          if (contentElement == null)
            o.print("    null");
          else
            o.print("    \"" + convertToJavaString(contentElement.toString())
                    + "\"");
          o.println(",");
        }
      for (int i = 0; i < appendNull; i++)
        o.println("    null,");
      o.println("  };");
    }

    public boolean isUsable()
    {
      return true;
    }
  }

  class TimeZoneContent implements JavaContent
  {
    ListDataElement listElt;
    boolean usable;

    public TimeZoneContent(ListDataElement elt)
    {
      this.listElt = elt;
      usable = false;
    }

    public boolean isPackage()
    {
      return true;
    }

    public String getName()
    {
      return "zoneStrings";
    }

    public String getData()
    {
      return null;
    }

    public void generateContent(PrintWriter o)
    {
      Enumeration<String> keys = listElt.listData.keys();
      StringBuffer buffer = new StringBuffer();

      buffer.append("  private static final String[][] zoneStrings =\n");
      buffer.append("  {\n");

      while (keys.hasMoreElements())
        {
          String zoneName = keys.nextElement();
          Hashtable<String,Element> zoneTable;
          DataElement zoneData;
          StringBuffer buffer2 = new StringBuffer();
          boolean zoneDataFound = false;

          buffer2.append("    { ");

          buffer2.append(" \"" + zoneName + "\", ");

          zoneTable = listElt.flattenLeaf(zoneName);
          for (int j = 0; j < classpathZoneOrder.length; j++)
          {
            zoneData = (DataElement) zoneTable.get(classpathZoneOrder[j]);
            if (zoneData != null)
              {
                buffer2.append("\"");
                buffer2.append(convertToJavaString(zoneData.data));
                buffer2.append("\", ");
                zoneDataFound = true;
              }
            else
              /* TODO: Emit a warning here "Insufficient data" */
              buffer2.append("\"\", ");
          }
          if (zoneDataFound)
            {
              buffer.append(buffer2.substring(0, buffer2.length() - 2));
              buffer.append("},\n");
              usable = true;
            }
        }
      if (usable)
        {
          o.print(buffer);
          o.println("  };");
        }
    }

    public boolean isUsable()
    {
      return usable;
    }
  }

  class HashtableContent implements JavaContent
  {
    private String name;
    private Hashtable<String,Object> table;

    public HashtableContent(String name, Hashtable<String,Object> table)
    {
      this.name = name;
      this.table = table;
    }

    public boolean isPackage()
    {
      return true;
    }

    public String getName()
    {
      return name;
    }

    public String getData()
    {
      return null;
    }

    public void generateContent(PrintWriter o)
    {
      o.print("  private static final String " + name + "Keys = \"");
      Enumeration<String> keys = table.keys();
      boolean more = keys.hasMoreElements();
      while (more)
        {
          String key = keys.nextElement();
          if (key.indexOf("\u00A6") != -1)
            {
              System.err.println(name + " key: '" + key + "' contains \u00A6");
              System.exit(-1);
            }
          o.print(key);
          more = keys.hasMoreElements();
          if (more)
            o.print("\\u00A6");
        }
      o.println("\";");
      o.println();
      o.print("  private static final String " + name + "Values = \"");
      keys = table.keys();
      more = keys.hasMoreElements();
      while (more)
        {
          String key = keys.nextElement();
          String value = (String) table.get(key);
          value = convertToJavaString(value);
          if (value.indexOf("\u00A6") != -1)
            {
              System.err.println(name + " value: '" + value + "' contains \u00A6");
              System.exit(-1);
            }
          o.print(value);
          more = keys.hasMoreElements();
          if (more)
            o.print("\\u00A6");
        }
      o.println("\";");
      o.println();
      o.println("  private static final Hashtable " + name + ";");
      o.println("  static");
      o.println("  {");
      o.println("    " + name + " = new Hashtable();");
      o.println("    Enumeration keys = new StringTokenizer(" + name
                + "Keys, \"\\u00A6\");");
      o.println("    Enumeration values = new StringTokenizer(" + name
                + "Values, \"\\u00A6\");");
      o.println("    while (keys.hasMoreElements())");
      o.println("      {");
      o.println("         String key = (String) keys.nextElement();");
      o.println("         String value = (String) values.nextElement();");
      o.println("         " + name + ".put(key, value);");
      o.println("      }");
      o.println("  }");
    }

    public boolean isUsable()
    {
      return true;
    }
  }

  /*
   * Main body of the Java Locale generator.
   */
  public JavaGenerator(String inPackage, Collection<Analyzer> analyzers, String locale)
  {
    this.analyzers = analyzers;
    this.inPackage = inPackage;
    this.locale = locale;
  }

  public void addStringContent(Hashtable<String,Element> tree, String ref, String name)
  {
    DataElement data_elt = (DataElement) tree.get(ref);
    if (data_elt == null)
      return;
    localeContents.add(new StringContent(name, data_elt.data));
  }

  public void addOrderedListContent(Hashtable<String,Element> tree, String ref, String name,
                                    String[] order, int prependNull,
                                    int appendNull)
  {
    ListDataElement data_elt = (ListDataElement) tree.get(ref);
    if (data_elt == null)
      return;
    localeContents.add(new OrderedListContent(name, data_elt.listData, order,
                                              prependNull, appendNull));
  }

  private void computeCalendar(Hashtable<String,Element> flattree)
  {
    ListDataElement calendarElement;
    calendarElement = (ListDataElement) flattree.get("ldml.dates.calendars");
    if (calendarElement != null)
      {
        // GNU Classpath only supports gregorian calendar ATM. We will upgrade
        // the code
        // once it has been done in GNU Classpath.
        Hashtable<String,Element> calendarLeaf = calendarElement.flattenLeaf("gregorian");
        int i = 0;
        if (calendarLeaf == null)
          return;
        addOrderedListContent(
                              calendarLeaf,
                              "calendar.months.monthContext.monthWidth.abbreviated",
                              "shortMonths", gnu.ldml.Constants.monthsOrder[i],
                              0, 1);
        addOrderedListContent(calendarLeaf,
                              "calendar.months.monthContext.monthWidth.wide",
                              "months", gnu.ldml.Constants.monthsOrder[i], 0, 1);
        addOrderedListContent(calendarLeaf,
                              "calendar.days.dayContext.dayWidth.abbreviated",
                              "shortWeekdays", gnu.ldml.Constants.daysOrder, 1,
                              0);
        addOrderedListContent(calendarLeaf,
                              "calendar.days.dayContext.dayWidth.wide",
                              "weekdays", gnu.ldml.Constants.daysOrder, 1, 0);
        /* WEEKS */
        Element minDays = calendarLeaf.get("calendar.week.minDays");
        if(minDays != null)
          localeContents
            .add(new StringContent("minNumberOfDaysInFirstWeek", minDays.defaultType));

        Element firstDay = calendarLeaf.get("calendar.week.firstDay");
        if(firstDay != null)
          localeContents
            .add(new StringContent("firstDayOfWeek", firstDay.defaultType));

        /* ERAS */
        ListDataElement eraElement = (ListDataElement) calendarLeaf
          .get("calendar.eras.eraAbbr");
        if (eraElement != null)
          {
            String ac = (String) eraElement.listData.get("0");
            String bc = (String) eraElement.listData.get("1");
            if (ac != null && bc != null)
              localeContents
                .add(new ManualListContent("eras", new Object[] { ac, bc }));
          }
        DataElement amElement, pmElement;
        /* AM-PM */
        amElement = (DataElement) calendarLeaf.get("calendar.am");
        pmElement = (DataElement) calendarLeaf.get("calendar.pm");
        if (amElement != null && pmElement != null)
          {
            localeContents
              .add(new ManualListContent("ampms",
                                         new Object[] { amElement.data,
                                                       pmElement.data }));
          }
        /* Compute all date formats */
        ListDataElement dateFormats = (ListDataElement) calendarLeaf
          .get("calendar.dateFormats");
        if (dateFormats != null)
          {
            for (int j = 0; j < gnu.ldml.Constants.dateFormats.length; j++)
              {
                Hashtable<String,Element> dateFormat = dateFormats
                  .flattenLeaf(gnu.ldml.Constants.dateFormats[j]);
                if (dateFormat == null)
                  continue;
                addStringContent(dateFormat,
                                 "dateFormatLength.dateFormat.pattern",
                                 classpathDateFormats[j]);
              }
          }
        /* Compute all time formats */
        ListDataElement timeFormats = (ListDataElement) calendarLeaf
          .get("calendar.timeFormats");
        if (timeFormats != null)
          {
            for (int j = 0; j < gnu.ldml.Constants.timeFormats.length; j++)
              {
                Hashtable<String,Element> timeFormat = timeFormats
                  .flattenLeaf(gnu.ldml.Constants.timeFormats[j]);
                if (timeFormat == null)
                  continue;
                addStringContent(timeFormat,
                                 "timeFormatLength.timeFormat.pattern",
                                 classpathTimeFormats[j]);
              }
          }
      }
  }

  private void computeCollations(Hashtable<String,Element> flattree)
  {
    ListDataElement collations = (ListDataElement) flattree
      .get("ldml.collations");
    if (collations == null)
      return;
    Hashtable<String,Element> table = collations.flattenLeaf("standard");
    if (table == null)
      return;
    System.err.println("Found UCA table for collation rules");
    OrderedListElement listElt = (OrderedListElement) table
      .get("collation.rules");
    if (listElt == null)
      return;
    System.err.println("Found rules");
    CollationInterpreter interp = new CollationInterpreter(listElt.listData);
    interp.compute();
    localeContents.add(new BigStringContent("collation_rules", interp
      .toCollationRule()));
  }

  private void computeTimeZones(Hashtable<String,Element> flattree)
  {
    Element elt = flattree.get("ldml.dates.timeZoneNames");
    if (elt != null)
      localeContents.add(new TimeZoneContent((ListDataElement) elt));
  }

  private void computeLocalNames(Hashtable<String,Element> flattree)
  {
    ListDataElement elt = (ListDataElement) flattree
      .get("ldml.localeDisplayNames.territories");
    if (elt != null)
      localeContents.add(new HashtableContent("territories", elt.listData));
    elt = (ListDataElement) flattree.get("ldml.localeDisplayNames.languages");
    if (elt != null)
      localeContents.add(new HashtableContent("languages", elt.listData));
    elt = (ListDataElement) flattree.get("ldml.localeDisplayNames.variants");
    if (elt != null)
      localeContents.add(new HashtableContent("variants", elt.listData));
  }

  private void computeCurrencies(Hashtable<String,Element> flattree)
  {
    ListDataElement elt = (ListDataElement) flattree
      .get("ldml.numbers.currencies");
    if (elt == null)
      return;
    Enumeration<String> currencyKeys = elt.listData.keys();
    Hashtable<String,Object> currencyName = new Hashtable<String,Object>();
    Hashtable<String,Object> currencySymbol = new Hashtable<String,Object>();
    while (currencyKeys.hasMoreElements())
      {
        String code = currencyKeys.nextElement();
        Hashtable<String,Element> currencyTable = elt.flattenLeaf(code);
        DataElement displayName = (DataElement) currencyTable
          .get("currency.displayName");
        DataElement symbol = (DataElement) currencyTable.get("currency.symbol");
        if (displayName != null)
          currencyName.put(code, displayName.data);
        if (symbol != null)
          currencySymbol.put(code, symbol.data);
      }
    localeContents.add(new HashtableContent("currenciesDisplayName",
                                            currencyName));
    localeContents
      .add(new HashtableContent("currenciesSymbol", currencySymbol));
  }

  private void computeContents()
  {
    for (Iterator<Analyzer> i = analyzers.iterator(); i.hasNext();)
      {
        Analyzer analyzer = i.next();
        Hashtable<String,Element> flattree = analyzer.flattenTree();
        addStringContent(flattree, "ldml.numbers.symbols.percentSign",
                         "percent");
        addStringContent(flattree, "ldml.numbers.symbols.perMille", "perMill");
        addStringContent(flattree, "ldml.numbers.symbols.exponential",
                         "exponential");
        addStringContent(flattree, "ldml.numbers.symbols.infinity", "infinity");
        addStringContent(flattree, "ldml.numbers.symbols.nan", "NaN");
        addStringContent(flattree, "ldml.numbers.symbols.minusSign",
                         "minusSign");
        addStringContent(flattree, "ldml.numbers.symbols.nativeZeroDigit",
                         "zeroDigit");
        addStringContent(flattree, "ldml.numbers.symbols.decimal",
                         "decimalSeparator");
        addStringContent(flattree, "ldml.numbers.symbols.group",
                         "groupingSeparator");
        addStringContent(flattree, "ldml.numbers.symbols.patternDigit", "digit");
        addStringContent(
                         flattree,
                         "ldml.numbers.percentFormats.percentFormatLength.percentFormat.pattern",
                         "percentFormat");
        addStringContent(
                         flattree,
                         "ldml.numbers.currencyFormats.currencyFormatLength.currencyFormat.pattern",
                         "currencyFormat");
        addStringContent(flattree, "ldml.dates.localizedPatternChars",
                         "localPatternChars");
        computeCurrencies(flattree);
        computeCalendar(flattree);
        computeCollations(flattree);
        computeTimeZones(flattree);
        computeLocalNames(flattree);
      }
  }

  public void generateJavaHeader(PrintWriter o)
  {
    String suffix = ("root".equals(locale)) ? "" : "_" + locale;
    o.println("/* LocaleInformation" + suffix + ".java --");
    o.println("   Copyright (C) 2005  Free Software Foundation, Inc.");
    o.println();
    o.println("This file is part of GNU Classpath.");
    o.println();
    o
      .println("GNU Classpath is free software; you can redistribute it and/or modify");
    o
      .println("it under the terms of the GNU General Public License as published by");
    o
      .println("the Free Software Foundation; either version 2, or (at your option)");
    o.println("any later version.");
    o.println();
    o
      .println("GNU Classpath is distributed in the hope that it will be useful, but");
    o.println("WITHOUT ANY WARRANTY; without even the implied warranty of");
    o
      .println("MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU");
    o.println("General Public License for more details.");
    o.println();
    o
      .println("You should have received a copy of the GNU General Public License");
    o
      .println("along with GNU Classpath; see the file COPYING.  If not, write to the");
    o
      .println("Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA");
    o.println("02111-1307 USA.");
    o.println();
    o
      .println("Linking this library statically or dynamically with other modules is");
    o
      .println("making a combined work based on this library.  Thus, the terms and");
    o.println("conditions of the GNU General Public License cover the whole");
    o.println("combination.");
    o.println();
    o
      .println("As a special exception, the copyright holders of this library give you");
    o
      .println("permission to link this library with independent modules to produce an");
    o
      .println("executable, regardless of the license terms of these independent");
    o
      .println("modules, and to copy and distribute the resulting executable under");
    o
      .println("terms of your choice, provided that you also meet, for each linked");
    o
      .println("independent module, the terms and conditions of the license of that");
    o
      .println("module.  An independent module is a module which is not derived from");
    o
      .println("or based on this library.  If you modify this library, you may extend");
    o.println("this exception to your version of the library, but you are not");
    o.println("obligated to do so.  If you do not wish to do so, delete this");
    o.println("exception statement from your version. */");
    o.println();
    o.println();
    o
      .println("// This file was automatically generated by gnu.localegen from LDML");
    o.println();
    o.println("package " + inPackage + ';');
    o.println();
    o.println("import java.util.Enumeration;");
    o.println("import java.util.Hashtable;");
    o.println("import java.util.StringTokenizer;");
    o.println("import java.util.ListResourceBundle;");
    o.println();
  }

  public void generateContents(PrintWriter o)
  {
    o.println("  private static final Object[][] contents =");
    o.println("  {");
    for (int i = 0; i < localeContents.size(); i++)
      {
        JavaContent content = localeContents.get(i);
        if (content.isUsable())
          {
            if (content.isPackage())
              o.print("    { \"" + content.getName() + "\", " + content.getName()
                      + " }");
            else
              o.print("    { \"" + content.getName() + "\", \""
                      + convertToJavaString(content.getData()) + "\" }");
            o.println(",");
          }
      }
    o.println("  };");
    o.println();
    o.println("  public Object[][] getContents() { return contents; }");
  }

  public void generateJavaClass(PrintWriter o)
  {
    String suffix = ("root".equals(locale)) ? "" : "_" + locale;
    o.println("public class LocaleInformation" + suffix
              + " extends ListResourceBundle");
    o.println("{");
    for (int i = 0; i < localeContents.size(); i++)
      {
        JavaContent content = (JavaContent) localeContents.get(i);
        if (content.isPackage())
          {
            content.generateContent(o);
            o.println();
          }
      }
    generateContents(o);
    o.println("}");
  }

  public void generate(String path) throws IOException
  {
    if (path == null)
      path = ".";
    String relativePackagePath = inPackage.replace('.', File.separatorChar);
    File javaDir = new File(path, relativePackagePath);
    javaDir.mkdirs();
    String suffix = ("root".equals(locale)) ? "" : "_" + locale;
    File javaFile = new File(javaDir, "LocaleInformation" + suffix + ".java");
    computeContents();
    if (localeContents.size() == 0)
      {
        if (javaFile.exists())
          javaFile.delete();
        return;
      }
    FileWriter output = new FileWriter(javaFile);
    PrintWriter java_output = new PrintWriter(new BufferedWriter(output), true);
    generateJavaHeader(java_output);
    generateJavaClass(java_output);
  }
}

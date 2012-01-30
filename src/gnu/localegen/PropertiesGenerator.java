/*
 * gnu.localegen.PropertiesGenerator
 * Copyright (C) 2006, 2008, 2012 Free Software Foundation, Inc.
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
import gnu.ldml.DataElement;
import gnu.ldml.Element;
import gnu.ldml.Leaf;
import gnu.ldml.ListDataElement;
import gnu.ldml.OrderedListElement;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class PropertiesGenerator
{
  private static final int JAVA_HEXLENGTH = 4;
  private final Collection<Analyzer> analyzers;
  private final String locale;
  private final String inPackage;
  private List<JavaContent> localeContents = new ArrayList<JavaContent>();
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
  private static final String[] classpathMetazoneOrder = { "metazone.long.standard",
                                                      "metazone.short.standard",
                                                      "metazone.long.daylight",
                                                      "metazone.short.daylight" };
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
    StringBuilder buf = new StringBuilder();
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
      o.println(name + "=" + convertToJavaString(data));
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
      o.println(name + "=" + data);
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
    private String[] data;

    public ManualListContent(String name, String... data)
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
      o.print(name + "=");
      for (int i = 0; i < data.length; i++)
        {
          // FIXME: Don't print "\u00ae" after last entry.
          o.print(convertToJavaString(data[i].toString()) + "\\u00ae");
        }
      o.println();
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
    private Map<String,SortedSet<Leaf>> data;
    private String[] order;
    private int prependNull;
    private int appendNull;

    public OrderedListContent(String name, Map<String,SortedSet<Leaf>> data,
                              String[] order, int prependNull, int appendNull)
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
      // FIXME: Don't print "\u00ae" after last entry.
      o.print(name + "=");
      for (int i = 0; i < prependNull; i++)
        o.print("\\u00ae");
      for (int i = 0; i < order.length; i++)
        {
          SortedSet<Leaf> contentElement = data.get(order[i]);
          if (contentElement != null)
            o.print(convertToJavaString(contentElement.first().getData()));
          o.print("\\u00ae");
        }
      for (int i = 0; i < appendNull; i++)
        o.print("\\u00ae");
      o.println();
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
      int index;
      SortedSet<String> sortedKeys;

      sortedKeys = new TreeSet<String>();
      Iterator<String> unsortedKeys = listElt.elmKeys();
      while (unsortedKeys.hasNext())
        sortedKeys.add(unsortedKeys.next());
      Iterator<String> keys = sortedKeys.iterator();
      StringBuilder buffer = new StringBuilder();
      buffer.append("zoneStrings=");
      index = 0;

      while (keys.hasNext())
        {
          String zoneName = keys.next();
          Map<String,Element> zoneTable;
          DataElement zoneData;
          StringBuilder buffer2 = new StringBuilder();
          boolean zoneDataFound = false;

          buffer2.append(zoneName);
          buffer2.append("\\u00ae");

          zoneTable = listElt.flattenLeaf(zoneName);
          for (int j = 0; j < classpathZoneOrder.length; j++)
          {
            zoneData = (DataElement)zoneTable.get(classpathZoneOrder[j]);
            if (zoneData == null)
              zoneData = (DataElement) zoneTable.get(classpathMetazoneOrder[j]);
            if (zoneData != null)
              {
                buffer2.append(convertToJavaString(zoneData.data));
                zoneDataFound = true;
              }
            buffer2.append("\\u00ae");
          }
          if (zoneDataFound)
            {
              buffer.append(buffer2);
              buffer.append("\\u00a9");
              usable = true;
            }
          index++;
        }
      if (usable)
        {
          o.println(buffer);
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
    private SortedMap<String,SortedSet<Leaf>> table;

    public HashtableContent(String name, SortedMap<String,SortedSet<Leaf>> table)
    {
      this.name = name;
      this.table = table;
    }

    public HashtableContent(String name, Map<String,SortedSet<Leaf>> table)
    {
      this.name = name;
      this.table = new TreeMap<String,SortedSet<Leaf>>(table);
    }

    public HashtableContent(SortedMap<String,String> table, String name)
    {
      this.name = name;
      this.table = new TreeMap<String,SortedSet<Leaf>>();
      for (Map.Entry<String,String> entry : table.entrySet())
        {
          String key = entry.getKey();
          SortedSet<Leaf> set = this.table.get(key);
          if (set == null)
            {
              set = new TreeSet<Leaf>();
              this.table.put(key, set);
            }
          set.add(new Leaf(entry.getValue(), key));
        }
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
      Iterator<Map.Entry<String,SortedSet<Leaf>>> it = table.entrySet().iterator();
      while (it.hasNext())
        {
          Map.Entry<String,SortedSet<Leaf>> entry = it.next();
          String key = entry.getKey();
          String value = entry.getValue().first().getData();
          o.println(name + "." + key  + "=" + convertToJavaString(value));
        }
    }

    public boolean isUsable()
    {
      return true;
    }
  }

  /*
   * Main body of the Java Locale generator.
   */
  public PropertiesGenerator(String inPackage, Collection<Analyzer> analyzers, String locale)
  {
    this.analyzers = analyzers;
    this.inPackage = inPackage;
    this.locale = locale;
  }

  public void addCurrencyFormatContent(Map<String,Element> tree)
  {
    DataElement dataElt = (DataElement)
      tree.get("ldml.numbers.currencyFormats.currencyFormatLength.currencyFormat.pattern");

    if (dataElt == null)
      return;

    String data = dataElt.data;

    if (data.indexOf(";") == -1)
      data += ";-" + data;

    localeContents.add(new StringContent("currencyFormat", data));
  }

  public void addStringContent(Map<String,Element> tree, String ref, String name)
  {
    DataElement dataElt = (DataElement) tree.get(ref);
    if (dataElt == null)
      return;
    // Java doesn't have the 'v' pattern character so replace with z
    if (ref.startsWith("timeFormat"))
      dataElt.data = dataElt.data.replace('v','z').replace('V','z');
   localeContents.add(new StringContent(name, dataElt.data));
  }

  public void addOrderedListContent(Map<String,Element> tree, String ref, String name,
                                    String[] order, int prependNull,
                                    int appendNull)
  {
    ListDataElement dataElt = (ListDataElement) tree.get(ref);
    if (dataElt == null)
      return;
    localeContents.add(new OrderedListContent(name, dataElt.getData(), order,
                                              prependNull, appendNull));
  }

  private void computeCalendar(Map<String,Element> flattree)
  {
    ListDataElement calendarElement;
    calendarElement = (ListDataElement) flattree.get("ldml.dates.calendars");
    if (calendarElement != null)
      {
        // GNU Classpath only supports gregorian calendar ATM. We will upgrade
        // the code
        // once it has been done in GNU Classpath.
        Map<String,Element> calendarLeaf = calendarElement.flattenLeaf("gregorian");
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
            SortedSet<Leaf> ac = eraElement.getData("0");
            SortedSet<Leaf> bc = eraElement.getData("1");
            if (ac != null && bc != null)
              localeContents
                .add(new ManualListContent("eras", ac.first().getData(),
                                           bc.first().getData()));
          }
        DataElement amElement, pmElement;
        /* AM-PM */
        amElement = (DataElement) calendarLeaf.get("calendar.am");
        pmElement = (DataElement) calendarLeaf.get("calendar.pm");
        if (amElement != null && pmElement != null)
          {
            localeContents
              .add(new ManualListContent("ampms", amElement.data,
                                         pmElement.data));
          }
        /* Compute all date formats */
        ListDataElement dateFormats = (ListDataElement) calendarLeaf
          .get("calendar.dateFormats");
        if (dateFormats != null)
          {
            for (int j = 0; j < gnu.ldml.Constants.dateFormats.length; j++)
              {
                Map<String,Element> dateFormat = dateFormats
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
                Map<String,Element> timeFormat = timeFormats
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

  private void computeCollations(Map<String,Element> flattree)
  {
    ListDataElement collations = (ListDataElement) flattree
      .get("ldml.collations");
    if (collations == null)
      return;
    Map<String,Element> table = collations.flattenLeaf("standard");
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

  private void computeTimeZones(Map<String,Element> flattree)
  {
    Element elt = flattree.get("ldml.dates.timeZoneNames");
    if (elt != null)
      localeContents.add(new TimeZoneContent((ListDataElement) elt));
  }

  private void computeLocalNames(Map<String,Element> flattree)
  {
    ListDataElement elt = (ListDataElement) flattree
      .get("ldml.localeDisplayNames.territories");
    if (elt != null)
      localeContents.add(new HashtableContent("territories", elt.getData()));
    elt = (ListDataElement) flattree.get("ldml.localeDisplayNames.languages");
    if (elt != null)
      localeContents.add(new HashtableContent("languages", elt.getData()));
    elt = (ListDataElement) flattree.get("ldml.localeDisplayNames.variants");
    if (elt != null)
      localeContents.add(new HashtableContent("variants", elt.getData()));
  }

  private void computeCurrencies(Map<String,Element> flattree)
  {
    ListDataElement elt = (ListDataElement) flattree
      .get("ldml.numbers.currencies");
    if (elt == null)
      return;
    Iterator<String> currencyKeys = elt.elmKeys();
    SortedMap<String,String> currencyName = new TreeMap<String,String>();
    SortedMap<String,String> currencySymbol = new TreeMap<String,String>();
    while (currencyKeys.hasNext())
      {
        String code = currencyKeys.next();
        Map<String,Element> currencyTable = elt.flattenLeaf(code);
        DataElement displayName = (DataElement) currencyTable
          .get("currency.displayName");
        DataElement symbol = (DataElement) currencyTable.get("currency.symbol");
        if (displayName != null)
          currencyName.put(code, displayName.data);
        if (symbol != null)
          currencySymbol.put(code, symbol.data);
      }
    localeContents.add(new HashtableContent(currencyName, "currenciesDisplayName"));
    localeContents.add(new HashtableContent(currencySymbol, "currenciesSymbol"));
  }

  private void computeContents()
  {
    for (Iterator<Analyzer> i = analyzers.iterator(); i.hasNext();)
      {
        Analyzer analyzer = i.next();
        Map<String,Element> flattree = analyzer.flattenTree();
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
        addCurrencyFormatContent(flattree);
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
    o.println("# LocaleInformation" + suffix + ".properties --");
    o.println("# Copyright (C) 1991-2005 Unicode, Inc.");
    o.println("# All rights reserved. Distributed under the Terms of Use");
    o.println("# in http://www.unicode.org/copyright.html.");
    o.println("#");
    o.println("# This file was automatically generated by gnu.localegen from CLDR.");
    o.println();
  }

  public void generateJavaClass(PrintWriter o)
  {
    for (int i = 0; i < localeContents.size(); i++)
      {
        JavaContent content = localeContents.get(i);
        content.generateContent(o);
      }
  }

  public void generate(String path) throws IOException
  {
    if (path == null)
      path = ".";
    String relativePackagePath = inPackage.replace('.', File.separatorChar);
    File javaDir = new File(path, relativePackagePath);
    javaDir.mkdirs();
    String suffix = ("root".equals(locale)) ? "" : "_" + locale;
    File javaFile = new File(javaDir, "LocaleInformation" + suffix + ".properties");
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

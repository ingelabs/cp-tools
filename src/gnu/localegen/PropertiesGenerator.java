/*
 * gnu.localegen.PropertiesGenerator Copyright (C) 2006, 2008
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
import java.util.Map;
import gnu.ldml.Element;
import gnu.ldml.DataElement;
import gnu.ldml.ListDataElement;
import gnu.ldml.OrderedListElement;

public class PropertiesGenerator
{
  private static final int JAVA_HEXLENGTH = 4;
  private final Collection analyzers;
  private final String locale;
  private final String inPackage;
  private ArrayList localeContents = new ArrayList();
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
    private Hashtable data;
    private String[] order;
    private int prependNull;
    private int appendNull;

    public OrderedListContent(String name, Hashtable data, String[] order,
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
      // FIXME: Don't print "\u00ae" after last entry.
      o.print(name + "=");
      for (int i = 0; i < prependNull; i++)
	o.print("\\u00ae");
      for (int i = 0; i < order.length; i++)
	{
          Object contentElement = data.get(order[i]);
          if (contentElement != null)
            o.print(convertToJavaString(contentElement.toString()));
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

      Enumeration keys = listElt.listData.keys();
      StringBuffer buffer = new StringBuffer();
      buffer.append("zoneStrings=");
      index = 0;

      while (keys.hasMoreElements())
	{
	  String zoneName = (String)keys.nextElement();
	  Hashtable zoneTable;
	  Iterator allValues;
	  DataElement zoneData;
	  StringBuffer buffer2 = new StringBuffer();
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
    private Hashtable table;

    public HashtableContent(String name, Hashtable table)
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
      Iterator it = table.entrySet().iterator();
      while (it.hasNext())
	{
	  Map.Entry entry = (Map.Entry) it.next();
	  String key = (String) entry.getKey();
	  String value = (String) entry.getValue();
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
  public PropertiesGenerator(String inPackage, Collection analyzers, String locale)
  {
    this.analyzers = analyzers;
    this.inPackage = inPackage;
    this.locale = locale;
  }

  public void addCurrencyFormatContent(Hashtable tree)
  {
    DataElement data_elt =
      (DataElement) tree.get("ldml.numbers.currencyFormats.currencyFormatLength.currencyFormat.pattern");

    if (data_elt == null)
      return;
    
    String data = data_elt.data;

    if (data.indexOf(";") == -1)
      data += ";-" + data;
    
    localeContents.add(new StringContent("currencyFormat", data));
  }

  public void addStringContent(Hashtable tree, String ref, String name)
  {
    DataElement data_elt = (DataElement) tree.get(ref);
    if (data_elt == null)
      return;
    localeContents.add(new StringContent(name, data_elt.data));
  }

  public void addOrderedListContent(Hashtable tree, String ref, String name,
                                    String[] order, int prependNull,
                                    int appendNull)
  {
    ListDataElement data_elt = (ListDataElement) tree.get(ref);
    if (data_elt == null)
      return;
    localeContents.add(new OrderedListContent(name, data_elt.listData, order,
                                              prependNull, appendNull));
  }

  private void computeCalendar(Hashtable flattree)
  {
    ListDataElement calendarElement;
    calendarElement = (ListDataElement) flattree.get("ldml.dates.calendars");
    if (calendarElement != null)
      {
        // GNU Classpath only supports gregorian calendar ATM. We will upgrade
        // the code
        // once it has been done in GNU Classpath.
        Hashtable calendarLeaf = calendarElement.flattenLeaf("gregorian");
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
	Element minDays = (Element) calendarLeaf.get("calendar.week.minDays");
	if(minDays != null)
          localeContents
            .add(new StringContent("minNumberOfDaysInFirstWeek", minDays.defaultType));

	Element firstDay = (Element) calendarLeaf.get("calendar.week.firstDay");
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
                Hashtable dateFormat = dateFormats
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
                Hashtable timeFormat = timeFormats
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

  private void computeCollations(Hashtable flattree)
  {
    ListDataElement collations = (ListDataElement) flattree
      .get("ldml.collations");
    if (collations == null)
      return;
    Hashtable table = collations.flattenLeaf("standard");
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

  private void computeHashtable(String name, Hashtable table)
  {
    localeContents.add(new HashtableContent(name, table));
  }

  private void computeTimeZones(Hashtable flattree)
  {
    Element elt = (Element) flattree.get("ldml.dates.timeZoneNames");
    if (elt != null)
      localeContents.add(new TimeZoneContent((ListDataElement) elt));
  }

  private void computeLocalNames(Hashtable flattree)
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

  private void computeCurrencies(Hashtable flattree)
  {
    ListDataElement elt = (ListDataElement) flattree
      .get("ldml.numbers.currencies");
    if (elt == null)
      return;
    Enumeration currencyKeys = elt.listData.keys();
    Hashtable currencyName = new Hashtable();
    Hashtable currencySymbol = new Hashtable();
    while (currencyKeys.hasMoreElements())
      {
        String code = (String) currencyKeys.nextElement();
        Hashtable currencyTable = elt.flattenLeaf(code);
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
    for (Iterator i = analyzers.iterator(); i.hasNext();)
      {
        Analyzer analyzer = (Analyzer) i.next();
        Hashtable flattree = analyzer.flattenTree();
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
        JavaContent content = (JavaContent) localeContents.get(i);
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

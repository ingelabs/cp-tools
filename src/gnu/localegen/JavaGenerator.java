package gnu.localegen;

import gnu.ldml.Analyzer;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.Date;
import java.text.DateFormat;
import gnu.ldml.Element;
import gnu.ldml.DataElement;
import gnu.ldml.ListDataElement;
import gnu.ldml.OrderedListElement;

public class JavaGenerator
{
  private static final int JAVA_HEXLENGTH = 4;

  private Analyzer analyzer;
  private String inPackage;
  private ArrayList localeContents = new ArrayList();

  private static final String[] classpathDateFormats =
  {
    "shortDateFormat", "mediumDateFormat", "longDateFormat", "fullDateFormat"
  };

  private static final String[] classpathTimeFormats =
  {
    "shortTimeFormat", "mediumTimeFormat", "longTimeFormat", "fullTimeFormat"
  };

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
  }

  /*
   * Convert a UTF string in a java source compatible string.
   */
  static String convertToJavaString(String s)
  {
    int lenOnLine = 0;

    if (s == null)
      return "null";

    StringBuffer buf = new StringBuffer();
    
    for (int i = 0; i < s.length(); i++)
      {
	char c = s.charAt(i);
	// Transform non-ASCII character into an escaped unicode character.
	if (c > 127 || c == '"')
	    {
	      buf.append("\\u");
	      String hexString = Integer.toHexString((int)c);
	      for (int j = 0; j < JAVA_HEXLENGTH-hexString.length(); j++)
		buf.append('0');
	      buf.append(hexString);
	    }
	else if (c == '\\')
	  {
	    buf.append("\\\\");
	  }
	else
	  buf.append(c);
	
	if (lenOnLine >= 60)
	  {
	    buf.append("\"");
	  }

      }
    return buf.toString();
  }

  /*
   * This class implements JavaContent for simple string locale data.
   * The entry has the name "name" and contains the data specified by "data".
   */ 
  class StringContent implements JavaContent
  {
    private String name;
    private String data;

    public StringContent(String name, String data)
    {
      this.name = name;
      this.data = convertToJavaString(data);
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
  }

  /*
   * This class implements JavaContent for big string locale data.
   * The entry has the name "name" and contains the data specified by "data".
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
	      o.println('"');
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
  }

  /*
   * This class implements JavaContent for simple ordered list.
   * The lists are ordered according to their index in the array.
   * The class builds an external array of Object to store the list
   * and use the identifier name for both the locale id name and the java
   * object name.
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
      o.println("  private static final Object[] " + name + " = {");

      for (int i = 0; i < data.length; i++)
	{
	  o.print("    \"" + convertToJavaString(data[i].toString()) + "\"");
	  if (i == data.length-1)
	    o.println();
	  else
	    o.println(",");
	}

      o.println("  };");
    }  
  }

  /*
   * This class implements JavaContent for string ordered list.
   * The constructor accepts a hashtable and an array of strings to specify
   * the order. It can also optionally adds some "null" entries at the beginning
   * of the array with addNull.
   */
  class OrderedListContent implements JavaContent
  {
    private String name;
    private Hashtable data;
    private String[] order;
    private int addNull;

    public OrderedListContent(String name, Hashtable data, String[] order, int addNull)
    {
      this.name = name;
      this.data = data;
      this.order = order;
      this.addNull = addNull;
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
      o.println("  private static final Object[] " + name + " = {");

      for (int i = 0; i < addNull; i++)
	o.println("    null,");

      for (int i = 0; i < order.length; i++)
	{
	  o.print("    \"" + convertToJavaString(data.get(order[i]).toString()) + "\"");
	  if (i != order.length-1)
	    o.println(",");
	  else
	    o.println();
	}

      o.println("  };");
    }
  }

  class TimeZoneContent implements JavaContent
  {
    ListDataElement listElt;

    public TimeZoneContent(ListDataElement elt)
    {
      this.listElt = elt;
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
      Enumeration keys = listElt.listData.keys();
      boolean starting;

      o.println("  private static final String[][] zoneStrings =");
      o.println("  {");

      starting = true;
      while (keys.hasMoreElements())
	{
	  String zoneName = (String)keys.nextElement();
	  Hashtable zoneTable;
	  Iterator allValues;
	  DataElement alternate;

	  if (!starting)
	      o.println(",");
	  starting = false;

	  o.print("    { \"" + zoneName + "\"");
	  
	  zoneTable = listElt.flattenLeaf(zoneName);
	  allValues = zoneTable.values().iterator();
	  while (allValues.hasNext())
	    {
	      Element elt = (Element)allValues.next();

	      if (elt instanceof DataElement)
		o.print(", \"" + convertToJavaString(((DataElement)elt).data) + '"');
	    }

	  o.print(" }");
	}

      o.println();
      o.println("  };");
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
      o.println("  private static final class Hashtable" + name + " extends java.util.Hashtable");
      o.println("  {");

      o.println("    public Hashtable" + name + "()");
      o.println("      {");
      o.println("        super();");

      Enumeration keys = table.keys();

      while (keys.hasMoreElements())
	{
	  String key, value;

	  key = (String)keys.nextElement();
	  value = (String)table.get(key);
	  o.println("        put(\"" + key + "\", \"" + convertToJavaString(value) + "\");");
	}
      o.println("      }");

      o.println("  }");
      o.println();
      o.println("  private static final Object " + name + " = new Hashtable" + name + "();");
    }
  }

  /*
   * Main body of the Java Locale generator.
   */

  public JavaGenerator(String inPackage, Analyzer analyzer)
  {
    this.analyzer = analyzer;
    this.inPackage = inPackage;
  }

  public void addStringContent(Hashtable tree, String ref, String name)
  {
    DataElement data_elt = (DataElement)tree.get(ref);

    if (data_elt == null)
      return;

    localeContents.add(new StringContent(name, data_elt.data));
  }

  public void addOrderedListContent(Hashtable tree, String ref, String name, String[] order, 
				    int addNull)
  {
    ListDataElement data_elt = (ListDataElement)tree.get(ref);

    if (data_elt == null)
      return;

    localeContents.add(new OrderedListContent(name, data_elt.listData, order, addNull));
  }

  private void computeCalendar(Hashtable flattree)
  {
    ListDataElement calendarElement;

    calendarElement = (ListDataElement)flattree.get("ldml.dates.calendars");
    if (calendarElement != null)
      {
	// GNU Classpath only supports gregorian calendar ATM. We will upgrade the code
	// once it has been done in GNU Classpath.
	Hashtable calendarLeaf = calendarElement.flattenLeaf("gregorian");
	int i = 0;

	if (calendarLeaf == null)
	  return;
	
	addOrderedListContent(calendarLeaf,
			      "calendar.months.monthContext.monthWidth.abbreviated", "shortMonths",
			      gnu.ldml.Constants.monthsOrder[i], 0);
	addOrderedListContent(calendarLeaf, "calendar.months.monthContext.monthWidth.wide", "months",
			      gnu.ldml.Constants.monthsOrder[i], 0);
	
	addOrderedListContent(calendarLeaf, "calendar.days.dayContext.dayWidth.abbreviated", "shortWeekdays",
			      gnu.ldml.Constants.daysOrder, 1);
	addOrderedListContent(calendarLeaf, "calendar.days.dayContext.dayWidth.wide", "weekdays",
			      gnu.ldml.Constants.daysOrder, 1);
	
	DataElement amElement, pmElement;
	
	/* AM-PM */
	amElement = (DataElement)calendarLeaf.get("calendar.am");
	pmElement = (DataElement)calendarLeaf.get("calendar.pm");
	if (amElement != null && pmElement != null)
	  {
	    localeContents.add(new ManualListContent("ampms", 
						     new Object[] {
						       amElement.data, pmElement.data
						     }));
	  }

	/* Compute all date formats */
	ListDataElement dateFormats = (ListDataElement)calendarLeaf.get("calendar.dateFormats");
	if (dateFormats != null)
	  {
	    for (int j = 0; j < gnu.ldml.Constants.dateFormats.length; j++)
	      {
		Hashtable dateFormat = dateFormats.flattenLeaf(gnu.ldml.Constants.dateFormats[j]);

		if (dateFormat == null)
		  continue;

		addStringContent(dateFormat, "dateFormatLength.dateFormat.pattern", classpathDateFormats[j]);
	      }
	  }

	/* Compute all time formats */
	ListDataElement timeFormats = (ListDataElement)calendarLeaf.get("calendar.timeFormats");
	if (timeFormats != null)
	  {
	    for (int j = 0; j < gnu.ldml.Constants.timeFormats.length; j++)
	      {
		Hashtable timeFormat = timeFormats.flattenLeaf(gnu.ldml.Constants.timeFormats[j]);

		if (timeFormat == null)
		  continue;

		addStringContent(timeFormat, "timeFormatLength.timeFormat.pattern", classpathTimeFormats[j]);
	      }
	  }

	/* Now compute the era attribute */
	
      }
  }

  private static String fixForRuleBasedCollator(String s)
  {
    StringBuffer sbuf = null;
    boolean useSBUF = false;

    for (int i = 0; i < s.length(); i++)
      {
	char c = s.charAt(i);

	if (!useSBUF)
	  {
	    if ((c >= 0x0009 && c <= 0x000d) || (c >= 0x0020 && c <= 0x002F)
		|| (c >= 0x003A && c <= 0x0040) || (c >= 0x005B && c <= 0x0060)
		|| (c >= 0x007B && c <= 0x007E))
	    {
	      useSBUF = true;
	      sbuf = new StringBuffer();
	      sbuf.append('\'');
	      sbuf.append(s.substring(0, i+1));
	    }
	  }
	else
	  {
	    if (s.charAt(i) == '\'')
	      sbuf.append("''");
	    else
	      sbuf.append(s.charAt(i));
	  }
      }
    
    if (useSBUF)
      {
	sbuf.append('\'');
	return sbuf.toString();
      }
    else
      return s;
  }

  private void computeCollations(Hashtable flattree)
  {
    OrderedListElement listElt = (OrderedListElement)flattree.get("ldml.collations.collation.rules");

    if (listElt == null)
      return;
      
    ArrayList listData = listElt.listData;
    StringBuffer rules = new StringBuffer();
    char ruleCharacter;
    boolean multiChar, prepend;

    for (int i = 0; i < listData.size(); i++)
      {
	DataElement elt = (DataElement)listData.get(i);

	if (elt.qualifiedName.equals("p"))
	  {
	    ruleCharacter = '<';
	    multiChar = false;
	    prepend = false;
	  }
	else if (elt.qualifiedName.equals("s"))
	  {
	    ruleCharacter = ';';
	    multiChar = false;
	    prepend = false;
	  }
	else if (elt.qualifiedName.equals("t"))
	  {
	    ruleCharacter = ',';
	    multiChar = false;
	    prepend = false;
	  }
	else if (elt.qualifiedName.equals("i"))
	  {
	    ruleCharacter = '=';
	    multiChar = false;
	    prepend = false;
	  }
	else if (elt.qualifiedName.equals("pc"))
	  {
	    ruleCharacter = '<';
	    multiChar = true;
	    prepend = false;
	  }
	else if (elt.qualifiedName.equals("sc"))
	  {
	    ruleCharacter = ';';
	    multiChar = true;
	    prepend = false;
	  }
	else if (elt.qualifiedName.equals("tc"))
	  {
	    ruleCharacter = ',';
	    multiChar = true;
	    prepend = false;
	  }
	else if (elt.qualifiedName.equals("ic"))
	  {
	    ruleCharacter = '=';
	    multiChar = true;
	    prepend = false;
	  }
	else if (elt.qualifiedName.equals("reset"))
	  {
	    ruleCharacter = '&';
	    multiChar = true;
	    prepend = false;
	  }
	else
	  continue;

	if (multiChar)
	  {
	    int insertPoint = prepend ? 0 : rules.length();

	    for (int j = 0; j < elt.data.length(); j++)
	      {
		rules.insert(insertPoint, ruleCharacter);
		insertPoint++;

		String s = fixForRuleBasedCollator(elt.data.substring(j, j+1));
		
		rules.insert(insertPoint, s);
		insertPoint += s.length();
	      }
	  }
	else
	  {
	    int insertPoint = prepend ? 0 : rules.length();

	    rules.insert(insertPoint, ruleCharacter);
	    insertPoint++;
	    
	    rules.insert(insertPoint, fixForRuleBasedCollator(elt.data));
	  }
      }
    
    localeContents.add(new BigStringContent("collation_rules", rules.toString()));
  }

  private void computeHashtable(String name, Hashtable table)
  {
    localeContents.add(new HashtableContent(name, table));
  }

  private void computeTimeZones(Hashtable flattree)
  {
    Element elt = (Element)flattree.get("ldml.dates.timeZoneNames");
    
    if (elt != null)
      localeContents.add(new TimeZoneContent((ListDataElement)elt));
  }

  private void computeLocalNames(Hashtable flattree)
  {
    ListDataElement elt = (ListDataElement)flattree.get("ldml.localeDisplayNames.territories");

    if (elt != null)
      localeContents.add(new HashtableContent("territories", elt.listData));
  }
  
  private void computeContents()
  {
    Hashtable flattree = analyzer.flattenTree();
    
    addStringContent(flattree, "ldml.numbers.symbols.percentSign", "percent");
    addStringContent(flattree, "ldml.numbers.symbols.perMille", "perMill");
    addStringContent(flattree, "ldml.numbers.symbols.exponential", "exponential");
    addStringContent(flattree, "ldml.numbers.symbols.infinity", "infinity");
    addStringContent(flattree, "ldml.numbers.symbols.nan", "NaN");
    addStringContent(flattree, "ldml.numbers.symbols.minusSign", "minusSign");
    addStringContent(flattree, "ldml.numbers.symbols.nativeZeroDigit", "zeroDigit");
    addStringContent(flattree, "ldml.numbers.symbols.decimal", "decimalSeparator");
    addStringContent(flattree, "ldml.numbers.symbols.group", "groupingSeparator");
    addStringContent(flattree, "ldml.numbers.symbols.patternDigit", "digit");
    addStringContent(flattree, "ldml.numbers.percentFormats.percentFormatLength.percentFormat.pattern", "percentFormat");
    addStringContent(flattree, "ldml.numbers.currencyFormats.currencyFormatLength.currencyFormat.pattern", "currencyFormat");
    addStringContent(flattree, "ldml.dates.localizedPatternChars", "localPatternChars");

    computeCalendar(flattree);
    computeCollations(flattree);
    computeTimeZones(flattree);
    computeLocalNames(flattree);
  }

  public void generateJavaHeader(PrintWriter o)
  {
    o.println("/* This file is generated by gnu.localegen from LDML " + analyzer.getParser().getName() + ".xml");
    o.println(" * Do not edit it manually !");
    o.println(" * Generated on " + DateFormat.getDateInstance().format(new Date()));
    o.println(" */");
    o.println();
    o.println("package " + inPackage + ';');
    o.println();
    o.println("import java.util.ListResourceBundle;");
    o.println();
  }

  public void generateContents(PrintWriter o)
  {
    o.println("  private static final Object[][] contents = ");
    o.println("  {");

    for (int i=0;i<localeContents.size();i++)
      {
	JavaContent content = (JavaContent)localeContents.get(i);
	
	if (content.isPackage())
	  o.print("    { \"" + content.getName() + "\", " + content.getName() + " }");
	else
	  o.print("    { \"" + content.getName() + "\", \"" + content.getData() + "\" }");

	if (i == localeContents.size()-1)
	  o.println();
	else
	  o.println(",");
      }

    o.println("  };");
    o.println();
    o.println("  public Object[][] getContents() { return contents; }");
  }

  public void generateJavaClass(PrintWriter o)
  {
    o.println("public class LocaleInformation_" + analyzer.getParser().getName() + " extends ListResourceBundle");
    o.println("{");

    computeContents();
    
    for (int i=0;i<localeContents.size();i++)
      {
	JavaContent content = (JavaContent)localeContents.get(i);
	
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

    File javaFile = new File(javaDir, "LocaleInformation_" + analyzer.getParser().getName() + ".java");

    FileWriter output = new FileWriter(javaFile);
    PrintWriter java_output = new PrintWriter(new BufferedWriter(output), true);
    
    generateJavaHeader(java_output);
    generateJavaClass(java_output);
  }
}

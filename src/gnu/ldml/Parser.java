package gnu.ldml;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import java.util.HashMap;
import java.net.URL;
import java.io.IOException;

public class Parser extends DefaultHandler
{

  private int ignoreAll;
  private Element parentElement;
  public Element rootElement;
  private String name;
  private URL url;
  private ParserElement currentElement;
  
  /*
   * ============================================
   * Describe all LDML elements in the XML format
   * ============================================
   */

  abstract class ParserElement implements Cloneable {
    abstract public void start(String qName, Attributes atts)
      throws SAXException;
    abstract public void end(String qName);
    abstract public void characters(char[] ch, int start, int length);

    public ParserElement previousElement;
  }

  class EmptyParserElement extends ParserElement {
    public void start(String qName, Attributes atts)
      throws SAXException
    {
    }

    public void characters(char[] ch, int start, int length)
    {
    }

    public void end(String qName)
    {
    }
  }

  /*
   * Element ignoring subtree
   */
  class Ignore extends ParserElement {
    public void start(String qName, Attributes atts)
    {
      ignoreAll++;
    }
    
    public void end(String qName)
    {
      ignoreAll--;
    }

    public void characters(char[] ch, int start, int length)
    {
    }
  }


  /*
   * Aliasing element.
   * It describes the "alias" tag. We chose to
   * put an LDML alias object in the tree this alias
   * will be solved and analyzed once the XML file has been
   * entirely parsed.
   */
  class Alias extends EmptyParserElement {
    public void start(String qName, Attributes atts)
      throws SAXException
    {
      AliasElement elt = new AliasElement(Parser.this, parentElement, qName);

      elt.aliasing = atts.getValue("source");
      elt.replacingElement = atts.getValue("type");
      parentElement = elt;
      super.start(qName, atts);
    }

    public void end(String qName)
    {
      parentElement = parentElement.superElement;
      super.end(qName);
    }
  }

  /*
   * Root element. This is a representative of the <ldml> tag.
   * There should be only one tag of that sort in the XML file.
   * The node is attached to Element.ROOT.
   */
  class Root extends EmptyParserElement
  {
    public void start(String qName, Attributes atts)
      throws SAXException
    {
      if (parentElement != null)
	throw new SAXException("<ldml> tag has already been used");

      Element elt = new Element(Parser.this, Element.ROOT, qName);

      super.start(qName, atts);
      parentElement = elt;
      rootElement = elt;
    }
  }

  /*
   * List element. This a pure list element of a group list.
   * Data is stored using "characters" and pushed in the list of
   * the parent element (which should be a ListDataElement).
   */
  class List extends ParserElement
  {
    String typeName;
    StringBuffer listData;

    public void start(String qName, Attributes atts)
      throws SAXException
    {
      typeName = atts.getValue("type");
      if (typeName == null)
	throw new SAXException("<" + qName + "> must have a type attribute");
      listData = new StringBuffer();
    }

    public void characters(char[] ch, int start, int length)
    {
      listData.append(ch, start, length);
    }
    
    public void end(String qName)
    {
      ((ListDataElement)parentElement).listData.put(typeName, listData.toString());
    }
  }

  /*
   * Data element. This is a pure leaf node of the tree.
   */
  class Data extends ParserElement
  {
    StringBuffer data;
    DataElement elt;
    
    public void start(String qName, Attributes atts)
    {
      data = new StringBuffer();
      parentElement = elt = new DataElement(Parser.this, parentElement, qName);
    }

    public void characters(char[] ch, int start, int length)
    {
      data.append(ch, start, length);
    }
    
    public void end(String qName)
    {
      elt.data = data.toString();
      parentElement = elt.superElement;
    }
  }
  
  /*
   * Group element. This is a simple node with an option 
   * type attribute.
   */
  class Group extends EmptyParserElement
  {
    Element elt;

    public void start(String qName, Attributes atts)
      throws SAXException
    {
      super.start(qName, atts);

      String tname = atts.getValue("type");
      parentElement = elt = new Element(Parser.this, parentElement, qName);
      elt.defaultType = tname;
    }

    public void end(String qName)
    {
      parentElement = elt.superElement;
    }
  }

  /*
   * This is a group which contains a list of element indexed by their type.
   */
  class GroupList extends EmptyParserElement
  {
    ListDataElement elt;

    public void start(String qName, Attributes atts)
      throws SAXException
    {
      super.start(qName, atts);

      parentElement = elt = new ListDataElement(Parser.this, parentElement, qName);
    }

    public void end(String qName)
    {
      parentElement = elt.superElement;
    }
  }

  /*
   * This is a group which contains an ordered list of element.
   */
  class GroupOrderedList extends EmptyParserElement
  {
    OrderedListElement elt;

    public void start(String qName, Attributes atts)
      throws SAXException
    {
      super.start(qName, atts);

      parentElement = elt = new OrderedListElement(Parser.this, parentElement, qName);
    }

    public void end(String qName)
    {
      parentElement = elt.superElement;
    }
  }

  /*
   * This is a element of an ordered list.
   */
  class OrderedList extends EmptyParserElement
  {
    OrderedListElement parentList;
    StringBuffer listData = new StringBuffer();
    DataElement elt;
    
    public void start(String qName, Attributes atts)
      throws SAXException
    {
      super.start(qName, atts);

      listData.setLength(0);
      parentList = (OrderedListElement)parentElement;
      parentElement = elt = new DataElement(Parser.this, parentElement, qName);
    }

    public void end(String qName)
    {
      elt.data = listData.toString();
      parentElement = elt.superElement;
      parentList.listData.add(elt);
    }

    public void characters(char[] ch, int start, int length)
    {
      listData.append(ch, start, length);
    }    
  }
  
  /*
   * This is a list element. However the elements are always introduced in the main tree.
   * We use the typename to differentiate the various subtree.
   * <x type="y"> will produce ROOT.x.y
   */
  class SubGroupTypeList extends GroupList
  {
    Element monthelt;

    public void start(String qName, Attributes atts)
      throws SAXException
    {
      parentElement = monthelt = new Element(Parser.this, parentElement, qName);

      super.start(atts.getValue("type"), atts);
    }

    public void end(String qName)
    {
      super.end(qName);
      
      parentElement = monthelt.superElement;
    }
  }

  /*
   * This is a list element. The leaves are not attached directly to the
   * main tree but are stored in a separate list indexed using the type name.
   */
  class DetailedList extends EmptyParserElement
  {
    String typeName;
    StringBuffer listData;
    ListDataElement parentList;
    Element listElement;
    
    public void start(String qName, Attributes atts)
      throws SAXException
    {
      typeName = atts.getValue("type");
      parentList = (ListDataElement)parentElement;

      if (typeName == null)
	throw new SAXException("<" + qName + "> absolutely needs a type");

      listElement = new DetailedListElement(Parser.this, parentElement, qName, typeName);
      
      parentElement = listElement;
    }

    public void end(String qName)
    {
      parentElement = listElement.superElement;
    }
  }

  class GroupWithType extends Group
  {
    public void start(String qName, Attributes atts)
      throws SAXException
    {
      super.start(qName, atts);
      if (elt.defaultType == null)
	throw new SAXException("<" + qName + "> must have a type");
    }
  }
  
  /*
   * Specific element as it has two meanings depending on the parent element.
   */
  class Language extends ParserElement
  {
    List list;
    Data data;
    ParserElement elt;

    public void start(String qName, Attributes atts)
      throws SAXException
    {
      if (parentElement.qualifiedName.equals("identity"))
	{
	  elt = new GroupWithType();
	}
      else
	{
	  elt = new List();
	}

      elt.previousElement = previousElement;
      elt.start(qName, atts);
    }

    public void end(String qName)
    {
      elt.end(qName);
    }

    public void characters(char[] ch, int start, int length)
    {
      elt.characters(ch, start, length);
    }
  }

  class ListOrGroup extends ParserElement
  {
    List list;
    GroupWithType data;
    ParserElement elt;

    public void start(String qName, Attributes atts)
      throws SAXException
    {
      if (parentElement.qualifiedName.equals("identity"))
	{
	  elt = new GroupWithType();
	}
      else
	{
	  elt = new List();
	}

      elt.previousElement = previousElement;
      elt.start(qName, atts);
    }

    public void end(String qName)
    {
      elt.end(qName);
    }

    public void characters(char[] ch, int start, int length)
    {
      elt.characters(ch, start, length);
    }
  }

  /*
   * This is the main body of the content handler
   */
  HashMap allElements = new HashMap();

  public Parser()
  {
    allElements.put("ldml", new Root());

    allElements.put("identity", new Group());
    allElements.put("alias", new Alias());
    allElements.put("localeDisplayNames", new Group());
    allElements.put("territory", new ListOrGroup());
    allElements.put("variant", new GroupWithType());
    allElements.put("script", new ListOrGroup());

    allElements.put("languages", new GroupList());
    allElements.put("language", new Language());

    allElements.put("scripts", new GroupList());

    allElements.put("variants", new GroupList());
    allElements.put("variant", new Language());

    allElements.put("keys", new GroupList());
    allElements.put("key", new List());

    allElements.put("types", new GroupList());
    allElements.put("type", new List());

    allElements.put("dates", new Group());
    allElements.put("calendars", new GroupList());
    allElements.put("calendar", new DetailedList());
    allElements.put("months", new Group());
    allElements.put("days", new Group());
    allElements.put("monthContext", new Group());
    allElements.put("dayContext", new Group());
    allElements.put("monthWidth", new SubGroupTypeList());
    allElements.put("dayWidth", new SubGroupTypeList());
    allElements.put("month", new List());
    allElements.put("day", new List());

    allElements.put("dateFormats", new GroupList());
    allElements.put("dateFormatLength", new DetailedList());
    allElements.put("dateFormat", new Group());
    allElements.put("pattern", new Data());
    allElements.put("default", new Group());

    allElements.put("timeFormats", new GroupList());
    allElements.put("timeFormatLength", new DetailedList());
    allElements.put("timeFormat", new Group());

    allElements.put("percentFormats", new Group());
    allElements.put("percentFormatLength", new Group());
    allElements.put("percentFormat", new Group());

    allElements.put("currencyFormats", new Group());
    allElements.put("currencyFormatLength", new Group());
    allElements.put("currencyFormat", new Group());

    allElements.put("numbers", new Group());
    allElements.put("symbols", new Group());
    allElements.put("decimal", new Data());
    allElements.put("group", new Data());
    allElements.put("list", new Data());
    allElements.put("percentSign", new Data());
    allElements.put("nativeZeroDigit", new Data());
    allElements.put("patternDigit", new Data());
    allElements.put("minusSign", new Data());
    allElements.put("exponential", new Data());
    allElements.put("perMille", new Data());
    allElements.put("infinity", new Data());
    allElements.put("nan", new Data());

    allElements.put("am", new Data());
    allElements.put("pm", new Data());

    allElements.put("localizedPatternChars", new Data());
    allElements.put("date", new Group());

    allElements.put("layout", new Ignore());
    allElements.put("special", new Ignore());

    allElements.put("collations", new Group());
    allElements.put("collation", new Group());
    allElements.put("rules", new GroupOrderedList());
    allElements.put("base", new Ignore());
    allElements.put("p", new OrderedList());
    allElements.put("pc", new OrderedList());
    allElements.put("s", new OrderedList());
    allElements.put("sc", new OrderedList());
    allElements.put("t", new OrderedList());
    allElements.put("tc", new OrderedList());
    allElements.put("i", new OrderedList());
    allElements.put("ic", new OrderedList());
    allElements.put("settings", new Data());

    allElements.put("timeZoneNames", new GroupList());
    allElements.put("zone", new DetailedList());
    allElements.put("long", new Group());
    allElements.put("short", new Group());
    allElements.put("generic", new Data());
    allElements.put("standard", new Data());
    allElements.put("daylight", new Data());
    allElements.put("examplarCity", new Data());

    allElements.put("territories", new GroupList());

    allElements.put("supplementalData", new Ignore());
  }

  public Element getParentElement()
  {
    return parentElement;
  }
  
  public void setName(String name)
  {
    this.name = name;
  }

  public String getName()
  {
    return name;
  }

  public void setURL(URL url)
  {
    this.url = url;
  }

  public URL getURL()
  {
    return url;
  }

  /*
   * ContentHandler inherited methods.
   */

  public void startDocument ()
  {
  }
  
  public void endDocument ()
  {
  }
  

  public void startElement (String uri, String name,
			    String qName, Attributes atts)
    throws SAXException
  {
    ParserElement elt = (ParserElement)allElements.get(name);

    if (elt == null)
      {
	ignoreAll++;
	return;
      }

    if (ignoreAll > 0 && !(elt instanceof Ignore))
      return;
    
    elt.previousElement = currentElement;
    currentElement = elt;
    
    elt.start(qName, atts);
  }
  
  public void endElement (String uri, String name, String qName)
    throws SAXException
  {
    ParserElement elt = (ParserElement)allElements.get(name);

    if (elt == null)
      {
	ignoreAll--;
	return;
      }

    if (ignoreAll > 0 && !(elt instanceof Ignore))
      return;

    if (elt != currentElement)
      throw new SAXException("invalid tag \"" + name + "\"");

    try
      {
	currentElement.end(qName);
      }
    catch (Exception e)
      {
	throw (SAXException)(new SAXException("unexpected exception for \"" + name + "\"").initCause(e));
      }

    currentElement = elt.previousElement;
    elt.previousElement = null;
  }

  public void characters(char[] ch, int start, int length)
  {
    currentElement.characters(ch, start, length);
  }


  public void parse(XMLReader reader) throws IOException, SAXException
  {
    reader.setContentHandler(this);
    reader.parse(new InputSource(url.openStream()));
  }
}

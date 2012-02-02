/*
 * gnu.ldml.Parser
 * Copyright (C) 2005, 2012 Free Software Foundation, Inc.
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
package gnu.ldml;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.net.URL;
import java.io.IOException;

public class Parser extends DefaultHandler
{
  private int ignoreAll;
  private Element parentElement;
  public Element rootElement;
  private URL url;
  private ParserElement currentElement;
  private String name;

  /*
   * ============================================ Describe all LDML elements in
   * the XML format ============================================
   */
  abstract class ParserElement implements Cloneable
  {
    abstract public void start(String qName, Attributes atts)
      throws SAXException;

    abstract public void end(String qName) throws SAXException;

    abstract public void characters(char[] ch, int start, int length);

    public ParserElement previousElement;
  }

  class EmptyParserElement extends ParserElement
  {
    public void start(String qName, Attributes atts) throws SAXException
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
  class Ignore extends ParserElement
  {
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
   * Aliasing element. It describes the "alias" tag. We chose to put an LDML
   * alias object in the tree this alias will be solved and analyzed once the
   * XML file has been entirely parsed.
   */
  class Alias extends EmptyParserElement
  {
    public void start(String qName, Attributes atts) throws SAXException
    {
      AliasElement elt = new AliasElement(Parser.this, parentElement, qName);
      elt.aliasing = atts.getValue("source");
      elt.replacingElement = atts.getValue("path");
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
   * Root element. This is a representative of the <ldml> tag. There should be
   * only one tag of that sort in the XML file. The node is attached to
   * Element.ROOT.
   */
  class Root extends EmptyParserElement
  {
    public void start(String qName, Attributes atts) throws SAXException
    {
      super.start(qName, atts);
      if (parentElement == null)
        {
          Element elt = new Element(Parser.this, Element.ROOT, qName);
          rootElement = elt;
          parentElement = elt;
        }
      else
        {
          parentElement = rootElement;
        }
    }
  }

  /*
   * List element. This a pure list element of a group list. Data is stored
   * using "characters" and pushed in the list of the parent element (which
   * should be a ListDataElement).
   */
  class List extends ParserElement
  {
    String typeName;
    StringBuffer listData;
    Draft draft;
    String altText;

    public void start(String qName, Attributes atts) throws SAXException
    {
      typeName = atts.getValue("type");
      if (typeName == null)
        throw new SAXException("<" + qName + "> must have a type attribute");
      draft = Draft.fromString(atts.getValue("draft"));
      altText = atts.getValue("alt");
      listData = new StringBuffer();
    }

    public void characters(char[] ch, int start, int length)
    {
      listData.append(ch, start, length);
    }

    public void end(String qName)
    {
      Leaf leaf = new Leaf(listData.toString(), typeName, draft, altText);
      ((ListDataElement) parentElement).addData(typeName, leaf);
    }
  }

  abstract class DataBase extends ParserElement
  {
    StringBuffer data = new StringBuffer();

    public void start(String qName, Attributes atts) throws SAXException
    {
      data.setLength(0);
    }

    public void characters(char[] ch, int start, int length)
    {
      data.append(ch, start, length);
    }
  }

  /*
   * Data element. This is a pure leaf node of the tree.
   */
  class Data extends DataBase
  {
    DataElement elt;

    public void start(String qName, Attributes atts) throws SAXException
    {
      super.start(qName, atts);
      parentElement = elt = new DataElement(Parser.this, parentElement, qName,
                                            Draft.fromString(atts.getValue("draft")),
                                            atts.getValue("alt"));
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
   * Group element. This is a simple node with an option type attribute.
   */
  class Group extends EmptyParserElement
  {
    Element elt;

    public void start(String qName, Attributes atts) throws SAXException
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
   * Node with a single attribute.
   */
  class SinglyAttributedNode extends EmptyParserElement
  {
    Element elt;
    String attrName;

    public SinglyAttributedNode(String attrName)
    {
      this.attrName = attrName;
    }

    public void start(String qName, Attributes atts) throws SAXException
    {
      super.start(qName, atts);
      String attr = atts.getValue(attrName);
      parentElement = elt = new Element(Parser.this, parentElement, qName);
      elt.defaultType = attr;
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

    public void start(String qName, Attributes atts) throws SAXException
    {
      super.start(qName, atts);
      parentElement = elt = new ListDataElement(Parser.this, parentElement,
                                                qName);
    }

    public void end(String qName)
    {
      parentElement = elt.superElement;
    }
  }

  class Collations extends GroupList
  {
    public void start(String qName, Attributes atts) throws SAXException
    {
      super.start(qName, atts);
      String vsl = atts.getValue("validSubLocales");
      if (vsl != null && vsl.length() > 0)
        {
          ListDataElement lde = (ListDataElement) parentElement;
          SortedSet<Leaf> old = lde.addData("validSubLocales",
                                            new Leaf("validSubLocales", vsl));
          if (old != null)
            throw new SAXException("validSubLocales already set to " + old);
        }
    }
  }

  /*
   * This is a group which contains an ordered list of element.
   */
  class GroupOrderedList extends EmptyParserElement
  {
    OrderedListElement elt;

    public void start(String qName, Attributes atts) throws SAXException
    {
      super.start(qName, atts);
      parentElement = elt = new OrderedListElement(Parser.this, parentElement,
                                                   qName);
    }

    public void end(String qName)
    {
      parentElement = elt.superElement;
    }
  }

  /*
   * This is a element of an ordered list.
   */
  class OrderedList extends Data
  {
    OrderedListBaseElement parentList;

    public void start(String qName, Attributes atts) throws SAXException
    {
      parentList = (OrderedListBaseElement) parentElement;
      super.start(qName, atts);
    }

    public void end(String qName)
    {
      super.end(qName);
      parentList.listData.add(elt);
    }
  }

  /*
   * This is a list element. However the elements are always introduced in the
   * main tree. We use the typename to differentiate the various subtree. <x
   * type="y"> will produce ROOT.x.y
   */
  class SubGroupTypeList extends GroupList
  {
    Element monthelt;

    public void start(String qName, Attributes atts) throws SAXException
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
   * This is a list element. The leaves are not attached directly to the main
   * tree but are stored in a separate list indexed using the type name.
   */
  class DetailedList extends EmptyParserElement
  {
    String typeName;
    StringBuffer listData;
    ListDataElement parentList;
    Element listElement;

    public void start(String qName, Attributes atts) throws SAXException
    {
      typeName = atts.getValue("type");
      parentList = (ListDataElement) parentElement;
      if (typeName == null)
        throw new SAXException("<" + qName + "> absolutely needs a type");
      listElement = new DetailedListElement(Parser.this, parentList, qName,
                                            typeName);
      parentElement = listElement;
    }

    public void end(String qName)
    {
      parentElement = listElement.superElement;
    }
  }

  /*
   * This parsing element make the presence of "type" imperative.
   */
  class GroupWithType extends Group
  {
    public void start(String qName, Attributes atts) throws SAXException
    {
      super.start(qName, atts);
      if (elt.defaultType == null)
        throw new SAXException("<" + qName + "> must have a type");
    }
  }

  /*
   * This is the reset element for collation rules.
   */
  class Reset extends DataBase
  {
    private ResetElement resetElement;
    private OrderedListElement parentList;
    private StringBuffer sb = new StringBuffer();

    public void start(String qName, Attributes atts) throws SAXException
    {
      parentList = (OrderedListElement) parentElement;
      resetElement = new ResetElement(Parser.this, parentElement, qName);
      super.start(qName, atts);
      String value;
      sb.setLength(0);
      value = atts.getValue("before");
      if (value != null)
        {
          if (value.equals("primary"))
            resetElement.before = ResetElement.BEFORE_PRIMARY;
          else if (value.equals("secondary"))
            resetElement.before = ResetElement.BEFORE_SECONDARY;
          else if (value.equals("tertiary"))
            resetElement.before = ResetElement.BEFORE_TERTIARY;
          else if (value.equals("identical"))
            resetElement.before = ResetElement.BEFORE_IDENTICAL;
          else
            throw new SAXException(
                                   "before only accept primary, secondary, tertiary or identical");
        }
      parentElement = resetElement;
    }

    public void end(String qName)
    {
      if (sb.length() != 0)
        resetElement.data = sb.toString();
      parentElement = resetElement.superElement;
      parentList.listData.add(resetElement);
    }

    public void characters(char ch[], int ofs, int len)
    {
      sb.append(ch, ofs, len);
    }
  }

  class Expansion extends ParserElement
  {
    ExpansionElement elt;

    public void start(String qName, Attributes atts) throws SAXException
    {
      elt = new ExpansionElement(Parser.this, parentElement, qName);
      parentElement = elt;
    }

    public void end(String qName) throws SAXException
    {
      parentElement = elt.superElement;
      elt.fixExpansionData();
    }

    public void characters(char[] ch, int ofs, int len)
    {
    }
  }

  class CP extends EmptyParserElement
  {
    public void start(String qName, Attributes atts) throws SAXException
    {
      String hex = atts.getValue("hex");
      char code;
      if (hex == null)
        throw new SAXException("<cp> needs an hex argument");
      if (!(previousElement instanceof DataBase))
        throw new SAXException("<cp> needs a data type element as parent");
      code = (char) Integer.parseInt(hex, 16);
      ((DataBase) previousElement).data.append(code);
    }

    public void end(String qName)
    {
    }
  }

  /*
   * This is specific to some elements which are both presents as an identity
   * element and as a list element. We use the context to chose the right
   * parsing element to use.
   */
  class ListOrGroup extends ParserElement
  {
    List list;
    GroupWithType data;
    ParserElement elt;

    public void start(String qName, Attributes atts) throws SAXException
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

    public void end(String qName) throws SAXException
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
  Map<String,ParserElement> allElements = new HashMap<String,ParserElement>();

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
    allElements.put("language", new ListOrGroup());
    allElements.put("scripts", new GroupList());
    allElements.put("variants", new GroupList());
    allElements.put("variant", new ListOrGroup());
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
    allElements.put("eras", new Group());
    allElements.put("eraAbbr", new GroupList());
    allElements.put("era", new List());
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
    allElements.put("week", new Group());
    allElements.put("minDays", new SinglyAttributedNode("count"));
    allElements.put("firstDay", new SinglyAttributedNode("day"));
    allElements.put("am", new Data());
    allElements.put("pm", new Data());
    allElements.put("localizedPatternChars", new Data());
    allElements.put("date", new Group());
    allElements.put("layout", new Ignore());
    allElements.put("special", new Ignore());
    allElements.put("collations", new Collations());
    allElements.put("collation", new DetailedList());
    allElements.put("rules", new GroupOrderedList());
    allElements.put("base", new Group());
    allElements.put("reset", new Reset());
    allElements.put("p", new OrderedList());
    allElements.put("pc", new OrderedList());
    allElements.put("s", new OrderedList());
    allElements.put("sc", new OrderedList());
    allElements.put("t", new OrderedList());
    allElements.put("tc", new OrderedList());
    allElements.put("i", new OrderedList());
    allElements.put("ic", new OrderedList());
    allElements.put("settings", new Data());
    allElements.put("x", new Expansion());
    allElements.put("extend", new Data());
    allElements.put("first_tertiary_ignorable", new Group());
    allElements.put("first_secondary_ignorable", new Group());
    allElements.put("first_primary_ignorable", new Group());
    allElements.put("last_tertiary_ignorable", new Group());
    allElements.put("last_secondary_ignorable", new Group());
    allElements.put("last_primary_ignorable", new Group());
    allElements.put("last_variable", new Group());
    allElements.put("last_non_ignorable", new Group());
    allElements.put("timeZoneNames", new GroupList());
    allElements.put("zone", new DetailedList());
    allElements.put("long", new Group());
    allElements.put("short", new Group());
    allElements.put("generic", new Data());
    allElements.put("standard", new Data());
    allElements.put("daylight", new Data());
    allElements.put("examplarCity", new Data());
    allElements.put("metazone", new DetailedList());
    allElements.put("commonlyUsed", new Group());
    allElements.put("special", new Group());
    allElements.put("territories", new GroupList());
    allElements.put("currencies", new GroupList());
    allElements.put("currency", new DetailedList());
    allElements.put("displayName", new Data());
    allElements.put("symbol", new Data());
    allElements.put("supplementalData", new Ignore());
    allElements.put("cp", new CP());
  }

  public Element getParentElement()
  {
    return parentElement;
  }

  public void setURL(URL url)
  {
    this.url = url;
  }

  public URL getURL()
  {
    return url;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public String getName()
  {
    return name;
  }

  /*
   * ContentHandler inherited methods.
   */
  public void startDocument()
  {
  }

  public void endDocument()
  {
  }

  public void startElement(String uri, String name, String qName,
                           Attributes atts) throws SAXException
  {
    ParserElement elt = allElements.get(name);
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

  public void endElement(String uri, String name, String qName)
    throws SAXException
  {
    ParserElement elt = allElements.get(name);
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
        throw (SAXException) (new SAXException("unexpected exception for \""
                                               + name + "\"").initCause(e));
      }
    currentElement = elt.previousElement;
    elt.previousElement = null;
  }

  public void characters(char[] ch, int start, int length)
  {
    if (currentElement != null)
      {
        currentElement.characters(ch, start, length);
      }
  }

  public void parse(XMLReader reader) throws IOException, SAXException
  {
    reader.setContentHandler(this);
    reader.parse(new InputSource(url.openStream()));
  }
}

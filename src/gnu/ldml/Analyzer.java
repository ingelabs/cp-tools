/*
 * gnu.ldml.Analyzer Copyright (C) 2004 Free Software Foundation, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.net.URL;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.XMLReader;
import org.xml.sax.SAXException;

public class Analyzer
{

  private static Hashtable flattenBranch(Element e)
  {
    Hashtable table = new Hashtable();
    ArrayList stack = new ArrayList();
    int stack_sz;
    stack.add(e);
    while (stack.size() != 0)
      {
        stack_sz = stack.size();
        for (int i = 0; i < stack_sz; i++)
          {
            Element elt = (Element) stack.get(i);
            if (elt.children.size() != 0)
              {
                stack.addAll(elt.children);
              }
            table.put(elt.getFullName(), elt);
          }
        stack.subList(0, stack_sz).clear();
      }
    return table;
  }
  private boolean is_collation;
  private Collection locales;
  private URL mainFile;
  private Parser mainParser;
  private Hashtable parserTable = new Hashtable();
  private Hashtable treeFlattened;

  public Analyzer(URL mainFile) throws IOException, ParseException
  {
    this.mainFile = mainFile;
    addResourceFile(mainFile);
    resolveDependencies();
  }

  private Parser addResourceFile(URL resourceFile) throws IOException,
    ParseException
  {
    Parser parser = new Parser();
    XMLReader reader;
    try
      {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true); // because we use localName
        SAXParser saxParser = factory.newSAXParser();
        reader = saxParser.getXMLReader();
      }
    catch (ParserConfigurationException e)
      {
        IOException e2 = new IOException("Error creating the SAX parser for "
                                         + resourceFile);
        e2.initCause(e);
        throw e2;
      }
    catch (SAXException e)
      {
        IOException e2 = new IOException("Error creating the XML reader for "
                                         + resourceFile);
        e2.initCause(e);
        throw e2;
      }
    if (mainParser == null)
      mainParser = parser;
    parser.setURL(resourceFile);
    String fileName = resourceFile.getFile();
    int idx, idx2;
    if ((idx = fileName.lastIndexOf(".xml")) < 0)
      throw new Error("file does not end with .xml");
    if ((idx2 = fileName.lastIndexOf("/")) < 0)
      idx2 = -1;
    parser.setName(fileName.substring(idx2 + 1, idx));
    try
      {
        parser.parse(reader);
      }
    catch (SAXException e)
      {
        ParseException e2 = new ParseException("Error reading XML source file "
                                               + resourceFile);
        e2.initCause(e);
        throw e2;
      }
    Hashtable table = flattenTree();
    locales = new HashSet();
    Element elt = (Element) table.get("ldml.identity.language");
    String mainIdentity;
    if (elt == null)
      throw new ParseException(
                               "No identity.language tag in XML. Cannot identify the resource file.");
    mainIdentity = elt.defaultType.intern();
    elt = (Element) table.get("ldml.identity.territory");
    if (elt != null)
      {
        mainIdentity += "_" + elt.defaultType;
        elt = (Element) table.get("ldml.identity.variant");
        if (elt != null)
          mainIdentity += "_" + elt.defaultType;
      }
    elt = (Element) table.get("ldml.identity.script");
    if (elt != null)
      mainIdentity += "_" + elt.defaultType;
    locales.add(mainIdentity);
    // Process ldml/collations@validSublocales
    ListDataElement collations = (ListDataElement) table.get("ldml.collations");
    if (collations != null)
      {
        String vsl = (String) collations.listData.get("validSubLocales");
        if (false && vsl != null) // disabled for the moment
          {
            StringTokenizer st = new StringTokenizer(vsl, " ");
            while (st.hasMoreTokens())
              {
                locales.add(st.nextToken());
              }
          }
        is_collation = true;
      }
    for (Iterator i = locales.iterator(); i.hasNext();)
      {
        String locale = (String) i.next();
        parserTable.put(locale, parser);
      }
    return parser;
  }

  private void buildAliasList(ArrayList alist, Parser p)
  {
    if (p.getParentElement() == null)
      return;
    ArrayList stack = new ArrayList();
    stack.add(p.getParentElement());
    while (stack.size() != 0)
      {
        int sz = stack.size();
        for (int i = 0; i < sz; i++)
          {
            Element e = (Element) stack.get(i);
            if (e instanceof AliasElement)
              alist.add(e);
            else
              stack.addAll(e.children);
          }
        stack.subList(0, sz).clear();
      }
  }

  private Element fetchResource(AliasElement alias) throws IOException,
    ParseException
  {
    Parser p = null;
    String resName = alias.aliasing;
    /*
     * First, we look for the resource file. The names are of the XXX_YYY
     * (recursively on XXX). If we fail on this file and there is no parse
     * exceptions, then we try XXX.
     */
    while (resName.length() != 0)
      {
        p = (Parser) parserTable.get(resName);
        if (p == null)
          {
            try
              {
                p = addResourceFile(new URL(alias.parentParser.getURL(),
                                            resName + ".xml"));
              }
            catch (ParseException e)
              {
                throw e;
              }
            catch (IOException e)
              {
                continue;
              }
            break;
          }
        int idx = resName.lastIndexOf('_');
        if (idx < 0)
          idx = 0;
        resName = resName.substring(0, idx);
      }
    /*
     * Now we have parsed the good resource file. We must go in the tree and
     * find the right element specified by the position and the argument of
     * AliasElement.
     */
    Hashtable table = flattenBranch(p.rootElement);
    String elementName = alias.superElement.getFullName();
    while (elementName.length() != 0)
      {
        Element e = (Element) table.get(elementName);
        if (e == null)
          {
            int idx = elementName.lastIndexOf('.');
            if (idx < 0)
              elementName = "";
            else
              elementName = elementName.substring(0, idx);
            continue;
          }
        /* We have found a candidate. Check if it is a list */
        if (!(e instanceof ListDataElement))
          {
            System.err.println("Incorrect aliasing element in "
                               + alias.parentParser.getName()
                               + " while looking in " + p.getName());
            return null;
          }
        /* It is a list element, look for the right sub-tree */
        ListDataElement lst = (ListDataElement) e;
        e = (Element) lst.listData.get(resName);
        if (e == null)
          throw new ParseException("Unknown aliasing element " + resName
                                   + " in " + p.getName());
        return e;
      }
    return null;
  }

  public Hashtable flattenTree()
  {
    if (treeFlattened != null)
      return treeFlattened;
    treeFlattened = flattenBranch(mainParser.rootElement);
    return treeFlattened;
  }

  public Collection getLocales()
  {
    return locales;
  }

  public boolean isCollation()
  {
    return is_collation;
  }

  private void resolveDependencies() throws IOException, ParseException
  {
    /*
     * Here we look for alias elements in the tree. We resolve dependencies
     * relatively to the main file.
     */
    Iterator i = parserTable.values().iterator();
    ArrayList aliasList = new ArrayList();
    HashMap pm = new HashMap();
    while (i.hasNext())
      {
        Parser p = (Parser) i.next();
        pm.put(p.getName(), p);
        buildAliasList(aliasList, p);
      }
    Iterator aliasIter = aliasList.iterator();
    while (aliasIter.hasNext())
      {
        AliasElement alias = (AliasElement) aliasIter.next();
        Parser aliasedParser;
        Element elt;
        elt = fetchResource(alias);
      }
  }
}
/*
 * gnu.ldml.Analyzer
 * Copyright (C) 2004, 2012 Free Software Foundation, Inc.
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
import java.util.List;
import java.util.Map;
import java.net.URL;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.XMLReader;
import org.xml.sax.SAXException;

public class Analyzer
{

  static Map<String,List<Element>> flattenBranch(Element e)
  {
    Map<String,List<Element>> table = new HashMap<String,List<Element>>();
    List<Element> stack = new ArrayList<Element>();
    int stackSize;
    stack.add(e);
    while (stack.size() != 0)
      {
        stackSize = stack.size();
        for (int i = 0; i < stackSize; i++)
          {
            Element elt = stack.get(i);
            if (elt != null)
              {
                if (elt.children.size() != 0)
                  {
                    stack.addAll(elt.children);
                  }
                String fullName = elt.getFullName();
                List<Element> elms = table.get(fullName);
                if (elms == null)
                  {
                    elms = new ArrayList<Element>();
                    table.put(elt.getFullName(), elms);
                  }
                boolean added = elms.add(elt);
                if (!added)
                  throw new Error("Couldn't add " + elt);
              }
          }
        stack.subList(0, stackSize).clear();
      }
    return table;
  }

  public static Element getSingleElement(List<Element> elms)
  {
    Element returnedElement = null;

    if (elms == null)
      return null;
    if (elms.size() > 1)
      {
        for (Element elm : elms)
          if (elm.getAltText().equals(""))
            returnedElement = elm;
        if (returnedElement == null)
          throw new IllegalArgumentException("No default element: " + elms);
        return returnedElement;
      }
    return elms.get(0);
  }

  private boolean is_collation;
  private Collection<String> locales;
  private Parser mainParser;
  private Hashtable<String,Parser> parserTable = new Hashtable<String,Parser>();
  private Map<String,List<Element>> treeFlattened;

  public Analyzer(URL mainFile) throws IOException, ParseException
  {
    addResourceFile(mainFile);
    // Don't bother with aliasing until not completely broken
    //resolveDependencies();
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
    Map<String,List<Element>> table = flattenTree();
    locales = new HashSet<String>();
    Element elt = getSingleElement(table.get("ldml.identity.language"));
    String mainIdentity;
    if (elt == null)
      throw new ParseException(
                               "No identity.language tag in XML. Cannot identify the resource file.");
    mainIdentity = elt.defaultType.intern();
    elt = getSingleElement(table.get("ldml.identity.territory"));
    if (elt != null)
      {
        mainIdentity += "_" + elt.defaultType;
        elt = getSingleElement(table.get("ldml.identity.variant"));
        if (elt != null)
          mainIdentity += "_" + elt.defaultType;
      }
    elt = getSingleElement(table.get("ldml.identity.script"));
    if (elt != null)
      mainIdentity += "_" + elt.defaultType;
    locales.add(mainIdentity);
    // Process ldml/collations@validSublocales
    ListDataElement collations = (ListDataElement)
      getSingleElement(table.get("ldml.collations"));
    if (collations != null)
      {
        /*
        String vsl = (String) collations.listData.get("validSubLocales");
        if (vsl != null) // disabled for the moment
          {
            StringTokenizer st = new StringTokenizer(vsl, " ");
            while (st.hasMoreTokens())
              {
                locales.add(st.nextToken());
              }
          }
        */
        is_collation = true;
      }
    for (Iterator<String> i = locales.iterator(); i.hasNext();)
      {
        String locale = i.next();
        parserTable.put(locale, parser);
      }
    return parser;
  }

  private void buildAliasList(List<AliasElement> alist, Parser p)
  {
    if (p.getParentElement() == null)
      return;
    ArrayList<Element> stack = new ArrayList<Element>();
    stack.add(p.getParentElement());
    while (stack.size() != 0)
      {
        int sz = stack.size();
        for (int i = 0; i < sz; i++)
          {
            Element e = stack.get(i);
            if (e instanceof AliasElement)
              alist.add((AliasElement) e);
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
    String replacement = alias.replacingElement;
    if (resName.equals("locale"))
      {
        // Special value, referring to the current locale
        p = alias.parentParser;
      }
    else
      {
        /*
         * First, we look for the resource file. The names are of the XXX_YYY
         * (recursively on XXX). If we fail on this file and there is no parse
         * exceptions, then we try XXX.
         */
        do
          {
            p = parserTable.get(resName);
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
                catch (FileNotFoundException e)
                  {
                    // Ignored; proceed to next file.
                  }
                catch (IOException e)
                  {
                    throw e;
                  }
                if (p == null)
                  {
                    // Alter resName to try again
                    int idx = resName.lastIndexOf('_');
                    if (idx < 0)
                      idx = 0;
                    resName = resName.substring(0, idx);
                  }
              }
          } while (p == null && resName.length() != 0);
      }
    if (p == null)
      {
        System.err.println("Could not resolve aliasing element: " + alias);
        return null;
      }
    /*
     * Now we have parsed the good resource file. We must go in the tree and
     * find the right element specified by the position and the argument of
     * AliasElement.
     */
    Map<String,List<Element>> table = flattenBranch(p.rootElement);
    String elementName = alias.superElement.getFullName();
    while (elementName.length() != 0)
      {
        Element e = getSingleElement(table.get(elementName));
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
            System.err.println("Incorrect aliasing element " + e.getFullName() + " in "
                               + alias.parentParser.getName()
                               + " while looking in " + p.getName());
            return null;
          }
        /* It is a list element, look for the right sub-tree */
        ListDataElement lst = (ListDataElement) e;
        e = lst.getElement(replacement);
        System.err.println("lst: " + lst);
        if (e == null)
          throw new ParseException("Unknown aliasing element " + replacement
                                   + " in " + p.getName());
        return e;
      }
    return null;
  }

  public Map<String,List<Element>> flattenTree()
  {
    if (treeFlattened != null)
      return treeFlattened;
    treeFlattened = flattenBranch(mainParser.rootElement);
    return treeFlattened;
  }

  public Collection<String> getLocales()
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
    Iterator<Parser> i = parserTable.values().iterator();
    ArrayList<AliasElement> aliasList = new ArrayList<AliasElement>();
    HashMap<String,Parser> pm = new HashMap<String,Parser>();
    while (i.hasNext())
      {
        Parser p = i.next();
        pm.put(p.getName(), p);
        buildAliasList(aliasList, p);
      }
    Iterator<AliasElement> aliasIter = aliasList.iterator();
    while (aliasIter.hasNext())
      {
        AliasElement alias = aliasIter.next();
        fetchResource(alias);
      }
  }
}

package gnu.ldml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Hashtable;
import java.net.URL;
import java.io.IOException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Analyzer 
{
  private Hashtable parserTable = new Hashtable();
  private Parser mainParser;
  private URL mainFile;
  private Hashtable treeFlattened;

  public Parser getParser()
  {
    return mainParser;
  }

  public Analyzer(URL mainFile) throws IOException, ParseException
  {
    this.mainFile = mainFile;
    addResourceFile(mainFile);
    resolveDependencies();
  }

  public Hashtable flattenTree()
  {
    if (treeFlattened != null)
      return treeFlattened;

    Hashtable table = new Hashtable();
    ArrayList stack = new ArrayList();
    int stack_sz;

    stack.add(mainParser.rootElement);
    while (stack.size() != 0)
      {
	stack_sz = stack.size();
	for (int i=0;i<stack_sz;i++)
	  {
	    Element elt = (Element)stack.get(i);
	    if (elt.children.size() != 0)
	      {
		stack.addAll(elt.children);
	      }
	    table.put(elt.getFullName(), elt);
	  }
	stack.subList(0, stack_sz).clear();	
      }

    treeFlattened = table;
    return table;
  }

  private void addResourceFile(URL resourceFile) throws IOException, ParseException
  {
    Parser parser = new Parser();
    XMLReader reader;

    try
      {
	reader = XMLReaderFactory.createXMLReader();
      }
    catch (SAXException e)
      {
	IOException e2 = new IOException("Error creating the XML reader for " + resourceFile);

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

    parser.setName(fileName.substring(idx2+1, idx));

    try
      {
	parser.parse(reader);
      }
    catch (SAXException e)
      {
	ParseException e2 = new ParseException("Error reading XML source file " + resourceFile);

	e2.initCause(e);
	throw e2;
      }

    Hashtable table = flattenTree();
    Element elt = (Element)table.get("ldml.identity.language");
    String fullIdentity;

    if (elt == null)
      throw new ParseException("No identity.language tag in XML. Cannot identify the resource file.");

    fullIdentity = elt.defaultType.intern();
    elt = (Element)table.get("ldml.identity.territory");
    if (elt != null)
      {
	fullIdentity += "_" + elt.defaultType;
	elt = (Element)table.get("ldml.identity.variant");
	if (elt != null)
	  fullIdentity += "_" + elt.defaultType;
      }
    
    elt = (Element)table.get("ldml.identity.script");
    if (elt != null)
      fullIdentity += "_" + elt.defaultType;

    parser.setName(fullIdentity);

    parserTable.put(parser.getName(), parser);
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
	for (int i=0;i<sz;i++)
	  {
	    Element e = (Element)stack.get(i);

	    if (e instanceof AliasElement)
	      alist.add(e);
	    else
	      stack.addAll(e.children);
	  }
	stack.subList(0, sz).clear();
      }
  }

  private Element fetchResource(AliasElement alias) throws IOException, ParseException
  {
    String resName = alias.aliasing;

    /*
     * First, we look for the resource file.
     * The names are of the XXX_YYY (recursively on XXX). If we fail on
     * this file and there is no parse exceptions, then we try XXX.
     */
    while (resName.length() != 0)
      {
	Parser p;
	
	p = (Parser) parserTable.get(resName);
	
	if (p == null)
	  {
	    try
	      {
		addResourceFile(new URL(alias.parentParser.getURL(), resName + ".xml"));
		p = (Parser)parserTable.get(resName);
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
     * Now we have parsed the good resource file. We must go in the tree
     * and find the right element specified by the position and the argument
     * of AliasElement.
     */

    return null;
  }
  
  private void resolveDependencies() throws IOException, ParseException
  {
    /*
     * Here we look for alias elements in the tree.
     * We resolve dependencies relatively to the main file.
     */
    Iterator i = parserTable.values().iterator();
    ArrayList aliasList = new ArrayList();
    HashMap pm = new HashMap();
    
    while (i.hasNext())
      {
	Parser p = (Parser)i.next();

	pm.put(p.getName(), p);
	buildAliasList(aliasList, p);
      }
    
    Iterator aliasIter = aliasList.iterator();
    while (aliasIter.hasNext())
      {
	AliasElement alias = (AliasElement)aliasIter.next();
	Parser aliasedParser;
	Element elt;

	elt = fetchResource(alias);
      }
  }
}

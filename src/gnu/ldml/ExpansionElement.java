package gnu.ldml;

import java.util.Iterator;
import org.xml.sax.SAXException;

public class ExpansionElement extends OrderedListBaseElement
{
  public String extendData;
  public String extendOperator;
  public String extendOperatorData;

  public ExpansionElement(Parser p, Element parent, String qName)
  {
    super(p, parent, qName);
  }

  void fixExpansionData()
    throws SAXException
  {
    /* Here we look for <extend> and assumes that any other operator is a collation rule */
    Iterator iter = children.iterator();

    while (iter.hasNext())
      {
	Element elt = (Element)iter.next();

	if (!(elt instanceof DataElement))
	  throw new SAXException("All children of " + qualifiedName + " should be data element");

	DataElement data_elt = (DataElement)elt;
	
	if (elt.qualifiedName.equals("extend"))
	  {
	    extendData = data_elt.data;
	  }
	else
	  {
	    if (extendOperator != null)
	      throw new SAXException("Only one collation rule by expansion element is authorized");

	    extendOperator = data_elt.qualifiedName;
	    extendOperatorData = data_elt.data;
	  }
      }
  }
}

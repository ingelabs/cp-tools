package gnu.ldml;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Enumeration;

public class ListDataElement extends Element {
  public Hashtable listData = new Hashtable();
  public String defaultLeaf;

  public ListDataElement(Parser p, Element parent, String name)
  {
    super(p, parent, name);
  }

  public void addChild(Element e)
  {
    if (e.qualifiedName.equals("default"))
      defaultLeaf = e.defaultType;
  }

  public Enumeration leaves()
  {
    return listData.keys();
  }

  public Hashtable flattenLeaf(String name)
  {
    Object listObject = listData.get(name);

    if (!(listObject instanceof Element))
      throw new Error("Cannot flatten a tree not constitued of Elements");
    
    Hashtable table = new Hashtable();
    ArrayList stack = new ArrayList();
    int stack_sz;

    stack.add(listObject);
    while (stack.size() != 0)
      {
	stack_sz = stack.size();
	for (int i=0;i<stack_sz;i++)
	  {
	    Element elt = (Element)stack.get(i);
	    if (elt.children.size() != 0)
	      {
		stack.addAll(elt.children);
		continue;
	      }
	    table.put(elt.getFullName(), elt);
	  }
	stack.subList(0, stack_sz).clear();	
      }

    return table;
  }
}

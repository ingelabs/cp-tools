package gnu.ldml;

import java.util.ArrayList;

public class OrderedListElement extends Element
{
  public ArrayList listData = new ArrayList();
  
  public OrderedListElement(Parser p, Element parent, String name)
  {
    super(p, parent, name);
  }

  public void addChild(Element e)
  {
    /* Do not register data elements */
    if (e instanceof DataElement)
      return;

    super.addChild(e);
  }
}

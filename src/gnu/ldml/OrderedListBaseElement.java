package gnu.ldml;

import java.util.ArrayList;

public class OrderedListBaseElement extends Element
{
  public ArrayList listData = new ArrayList();

  public OrderedListBaseElement(Parser p, Element parent, String qName)
  {
    super(p, parent, qName);
  }
}

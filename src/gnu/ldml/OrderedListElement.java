package gnu.ldml;

public class OrderedListElement extends OrderedListBaseElement
{
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
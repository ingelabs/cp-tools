package gnu.ldml;

public class DataElement extends Element
{
  public String data;

  public DataElement(Parser p, Element parent, String name)
  {
    super(p, parent, name);
  }
}
package gnu.ldml;

public class AliasElement extends Element
{
  public String aliasing;
  public String replacingElement;

  public AliasElement(Parser p, Element parent, String name)
  {
    super(p, parent, name);
  }
}

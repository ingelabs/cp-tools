package gnu.ldml;

public class DetailedListElement extends Element
{
  public DetailedListElement(Parser p, Element parent, String name, String typeName)
  {
    super(p, parent, name);

    ListDataElement plist = (ListDataElement) parent;

    plist.listData.put(typeName, this);
  }

  public String getFullName()
  {
    return qualifiedName;
  }
}

package gnu.ldml;

import java.util.ArrayList;

public class Element
{
  public static final Element ROOT = new Element();
  public ArrayList children = new ArrayList();
  public String defaultType;
  public String displayName;
  public Parser parentParser;
  public String qualifiedName;
  public Element superElement;

  private Element()
  {
  }

  public Element(Parser p, Element parent, String name)
  {
    parentParser = p;
    superElement = parent;
    qualifiedName = name;
    if (parent != ROOT)
      parent.addChild(this);
  }

  public void addChild(Element e)
  {
    children.add(e);
  }

  public String getFullName()
  {
    if (superElement == ROOT)
      return qualifiedName;
    return superElement.getFullName() + "." + qualifiedName;
  }
}
package gnu.ldml;

import java.util.ArrayList;

public class Element
{
  public static final Element ROOT = new Element();
  
  public Parser parentParser;
  public String displayName;
  public String qualifiedName;
  public String defaultType;
  public Element superElement;
  public ArrayList children = new ArrayList();

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

  public String getFullName()
  {
    if (superElement == ROOT)
      return qualifiedName;

    return superElement.getFullName() + "." + qualifiedName;
  }

  public void addChild(Element e)
  {
    children.add(e);
  }
}

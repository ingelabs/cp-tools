package gnu.ldml;

public class ResetElement extends Element
{
  public static final int NO_LOGIC = 0;
  public static final int FIRST_PRIMARY_IGNORABLE = 1;
  public static final int FIRST_SECONDARY_IGNORABLE = 2;
  public static final int FIRST_TERTIARY_IGNORABLE = 3;
  public static final int LAST_PRIMARY_IGNORABLE = 4;
  public static final int LAST_SECONDARY_IGNORABLE = 5;
  public static final int LAST_TERTIARY_IGNORABLE = 6;
  public static final int LAST_VARIABLE = 7;
  public static final int LAST_NON_IGNORABLE = 8;

  public static final int NO_BEFORE = 0;
  public static final int BEFORE_PRIMARY = 1;
  public static final int BEFORE_SECONDARY = 2;
  public static final int BEFORE_TERTIARY = 3;
  public static final int BEFORE_IDENTICAL = 4;

  public int before;
  public String data;
  public int logicalReset;

  public ResetElement(Parser p, Element parent, String name)
  {
    super(p, parent, name);

    logicalReset = NO_LOGIC;
  }
  
  public void addChild(Element e)
  {
    if (e.qualifiedName.equals("first_primary_ignorable"))
      logicalReset = FIRST_PRIMARY_IGNORABLE;
    else if (e.qualifiedName.equals("first_secondary_ignorable"))
      logicalReset = FIRST_SECONDARY_IGNORABLE;
    else if (e.qualifiedName.equals("first_tertiary_ignorable"))
      logicalReset = FIRST_TERTIARY_IGNORABLE;
    else if (e.qualifiedName.equals("last_primary_ignorable"))
      logicalReset = LAST_PRIMARY_IGNORABLE;
    else if (e.qualifiedName.equals("last_secondary_ignorable"))
      logicalReset = LAST_SECONDARY_IGNORABLE;
    else if (e.qualifiedName.equals("last_tertiary_ignorable"))
      logicalReset = LAST_TERTIARY_IGNORABLE;
    else if (e.qualifiedName.equals("last_variable"))
      logicalReset = LAST_VARIABLE;
    else if (e.qualifiedName.equals("last_non_ignorable"))
      logicalReset = LAST_NON_IGNORABLE;

    super.addChild(e);
  }
}

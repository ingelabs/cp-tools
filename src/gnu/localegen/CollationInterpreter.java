package gnu.localegen;

import gnu.ldml.Element;
import gnu.ldml.DataElement;
import gnu.ldml.ResetElement;
import gnu.ldml.ExpansionElement;
import java.util.ArrayList;
import java.util.Iterator;

public class CollationInterpreter
{

  class CollationElement
  {
    String code;
    CollationElement expand;
    int flags;
    boolean multiChar;
    int operator;
    CollationElement reset;
  }
  static final int EXPAND = 5;
  static final int EXPANSION = 4;
  static final int FIRST_RESET = 8;
  static final int IDENTICAL = 3;
  static final int IGNORABLE = 1;
  static final int INVALID_OPERATOR = -1;
  static final int NON_IGNORABLE = 2;
  static final int PRIMARY = 0;
  static final int RESET = 4;
  static final int SECONDARY = 1;
  static final int TERTIARY = 2;

  private static String fixForRuleBasedCollator(String s)
  {
    StringBuffer sbuf = null;
    boolean useSBUF = false;
    for (int i = 0; i < s.length(); i++)
      {
        char c = s.charAt(i);
        if (!useSBUF)
          {
            if ((c >= 0x0009 && c <= 0x000d) || (c >= 0x0020 && c <= 0x002F)
                || (c >= 0x003A && c <= 0x0040) || (c >= 0x005B && c <= 0x0060)
                || (c >= 0x007B && c <= 0x007E))
              {
                useSBUF = true;
                sbuf = new StringBuffer();
                sbuf.append('\'');
                sbuf.append(s.substring(0, i + 1));
              }
          }
        else
          {
            if (s.charAt(i) == '\'')
              sbuf.append("''");
            else
              sbuf.append(s.charAt(i));
          }
      }
    if (useSBUF)
      {
        sbuf.append('\'');
        return sbuf.toString();
      }
    else
      return s;
  }

  private static char getJavaOperator(int operator)
  {
    switch (operator)
      {
        case PRIMARY:
          return '<';
        case SECONDARY:
          return ';';
        case TERTIARY:
          return ',';
        case IDENTICAL:
          return '=';
        default:
          throw new AssertionError("Invalid operator code " + operator);
      }
  }

  ArrayList collationData;
  ArrayList expandedData = new ArrayList();

  public CollationInterpreter(ArrayList data)
  {
    this.collationData = data;
  }

  public void compute()
  {
    if (collationData == null)
      return;
    expandData();
    reorderData();
  }

  /*
   * This is the first pass. We translate the LDML rules and expand them a
   * little further. The expansion involves pre-analysis of RESET rules and
   * EXPAND rules. The output is an ordered array of collation rules with direct
   * access to expansion rules.
   */
  void expandData()
  {
    Iterator collat = collationData.iterator();
    int resetPosition;
    int lastNonIgnorable = 0;
    boolean newReset = true;
    CollationElement resetElement;
    resetElement = new CollationElement();
    resetElement.code = "";
    resetElement.flags |= FIRST_RESET;
    resetPosition = 0;
    while (collat.hasNext())
      {
        Element elt = (Element) collat.next();
        CollationElement e = new CollationElement();
        fillType(e, elt.qualifiedName);
        e.reset = resetElement;
        assert (e.operator != INVALID_OPERATOR);
        switch (e.operator)
          {
            case EXPAND:
              {
                /*
                 * Here we consider the expand type elements. They have a rule
                 * order specification (p, s, t, pc, sc, tc, i, ic) and an
                 * <extend> attribute. If we get two valid ones, we produce an
                 * Error.
                 * 
                 * Here we transform an EXPAND operator into a COLLATION
                 * operator and mark the rule as to be expanded.
                 */
                ExpansionElement expandElement = (ExpansionElement) elt;
                e.expand = new CollationElement();
                e.operator = INVALID_OPERATOR;
                e.flags |= EXPANSION;
                if (newReset)
                  {
                    e.flags |= FIRST_RESET;
                    newReset = false;
                  }
                e.expand.code = expandElement.extendData;
                fillType(e, expandElement.extendOperator);
                assert (e.operator != INVALID_OPERATOR);
                e.code = expandElement.extendOperatorData;
                expandedData.add(resetPosition, e);
                resetPosition++;
                if (lastNonIgnorable < resetPosition)
                  lastNonIgnorable++;
                break;
              }
            case RESET:
              {
                /*
                 * RESET needs also a special treatment. In Java, we have only
                 * automatic expansion and position reset. LDML may specific
                 * logical position reset: - various ignoreable positions -
                 * various non-ignoreable positions - normal symbol ordered
                 * reset (after or before) In the first case, we will place all
                 * elements as ignoreable and that's all for Java. The second
                 * case needs a bit of logic.
                 */
                ResetElement reset = (ResetElement) elt;
                switch (reset.logicalReset)
                  {
                    case ResetElement.FIRST_PRIMARY_IGNORABLE:
                    case ResetElement.FIRST_SECONDARY_IGNORABLE:
                    case ResetElement.FIRST_TERTIARY_IGNORABLE:
                    case ResetElement.LAST_PRIMARY_IGNORABLE:
                    case ResetElement.LAST_SECONDARY_IGNORABLE:
                    case ResetElement.LAST_TERTIARY_IGNORABLE:
                      e.flags |= IGNORABLE;
                      resetPosition = 0; // All ignorables are in heading
                                         // position in Java.
                      break;
                    case ResetElement.LAST_NON_IGNORABLE:
                      resetPosition = lastNonIgnorable;
                      break;
                    default:
                      {
                        /*
                         * We use in that case non-logical ordering We have to
                         * do as in Java and parse the entire list to check
                         * where to put the reset and the following elements.
                         */
                        assert (reset.data != null);
                        assert (!reset.data.equals(""));
                        resetElement.code = reset.data;
                        for (int i = 0; i < expandedData.size(); i++)
                          {
                            CollationElement ce = (CollationElement) expandedData
                              .get(i);
                            if (reset.data.equals(ce.code))
                              {
                                /*
                                 * We have found it ! Now we may have to move to
                                 * satisfy the "before" attribute.
                                 */
                                // TODO TODO
                                resetPosition = i;
                              }
                          }
                        break;
                      }
                  }
                resetElement = e;
                break;
              }
            default:
              /*
               * This is the default handler. If this is a sequence we expand
               * the list into single operation (e.multiChar == true). We mark
               * the first of the sequence as being a FIRST_RESET sequence if a
               * reset element has been issued.
               */
              {
                DataElement dat_elt = (DataElement) elt;
                if (e.multiChar)
                  {
                    // We have to completely expand the rule here.
                    for (int i = 0; i < dat_elt.data.length(); i++)
                      {
                        CollationElement e2 = new CollationElement();
                        e2.code = Character.toString(dat_elt.data.charAt(i));
                        e2.flags = (e.flags | e.reset.flags) & ~FIRST_RESET;
                        e2.reset = e.reset;
                        e2.operator = e2.operator;
                        if (newReset)
                          {
                            e2.flags |= FIRST_RESET;
                            newReset = false;
                          }
                        expandedData.add(resetPosition, e2);
                        resetPosition++;
                      }
                    if (lastNonIgnorable < resetPosition)
                      lastNonIgnorable += dat_elt.data.length();
                  }
                else
                  {
                    e.code = dat_elt.data;
                    e.flags = (e.flags | e.reset.flags) & ~FIRST_RESET;
                    expandedData.add(resetPosition, e);
                    resetPosition++;
                    if (newReset)
                      {
                        e.flags |= FIRST_RESET;
                        newReset = false;
                      }
                  }
                if (lastNonIgnorable < resetPosition)
                  lastNonIgnorable++;
              }
              break;
          }
      }
    /*
     * The collation data are now ready for reordering.
     */
    // Release the original array.
    collationData = null;
  }

  /*
   * This method fills the CollationElement with the correct type and type
   * information according to name. If it is unknown, the type is assigned to
   * INVALID_OPERATOR.
   */
  void fillType(CollationElement e, String name)
  {
    int type;
    boolean multiChar = false;
    if (name.equals("p"))
      {
        type = PRIMARY;
        multiChar = false;
      }
    else if (name.equals("s"))
      {
        type = SECONDARY;
        multiChar = false;
      }
    else if (name.equals("t"))
      {
        type = TERTIARY;
        multiChar = false;
      }
    else if (name.equals("i"))
      {
        type = IDENTICAL;
        multiChar = false;
      }
    else if (name.equals("pc"))
      {
        type = PRIMARY;
        multiChar = true;
      }
    else if (name.equals("sc"))
      {
        type = SECONDARY;
        multiChar = true;
      }
    else if (name.equals("tc"))
      {
        type = TERTIARY;
        multiChar = true;
      }
    else if (name.equals("ic"))
      {
        type = IDENTICAL;
        multiChar = true;
      }
    else if (name.equals("reset"))
      {
        type = RESET;
        multiChar = true;
      }
    else if (name.equals("x"))
      {
        type = EXPAND;
      }
    else
      type = INVALID_OPERATOR;
    e.operator = type;
    e.multiChar = multiChar;
  }

  /*
   * expandData has taken care of placing all ignorable sequence at the begining
   * (for Java) and the rest after. All sequences are marked (IGNORABLE,
   * NON_IGNORABLE or EXPAND). Here we will have to move EXPAND to the end and
   * finish the complete expansion according to the context.
   * 
   * At that stage may also occur CONTRACTION when a context element has been
   * encountered.
   */
  void reorderData()
  {
    String sequence_context = null;
    int originalSize = expandedData.size();
    int i = 0;
    while (i < originalSize)
      {
        CollationElement ce = (CollationElement) expandedData.get(i);
        if ((ce.flags & EXPANSION) != 0 && (ce.flags & IGNORABLE) == 0)
          {
            assert (sequence_context != null || (ce.reset.flags & FIRST_RESET) != 0);
            if ((ce.flags & FIRST_RESET) == 0)
              sequence_context = ce.reset.code;
            // We expand to remember the real position.
            ce.expand.code = sequence_context + ce.expand.code;
            // Now we move it to the end.
            expandedData.remove(i);
            originalSize--;
            expandedData.add(ce);
          }
        else
          {
            sequence_context = ce.code;
            i++;
          }
      }
  }

  public String toCollationRule()
  {
    Iterator iter = expandedData.iterator();
    boolean ignoreFinished = false;
    StringBuffer sb = new StringBuffer();
    while (iter.hasNext())
      {
        CollationElement ce = (CollationElement) iter.next();
        if ((ce.reset.flags & IGNORABLE) != 0)
          {
            // We don't know how to put EXPANSION in IGNORABLE atm.
            assert ((ce.flags & EXPANSION) == 0);
            assert (!ignoreFinished);
            sb.append('=');
            sb.append(fixForRuleBasedCollator(ce.code));
          }
        else if ((ce.flags & EXPANSION) != 0)
          {
            ignoreFinished = true;
            sb.append('&');
            sb.append(fixForRuleBasedCollator(ce.expand.code));
            sb.append(getJavaOperator(ce.operator));
            sb.append(fixForRuleBasedCollator(ce.code));
          }
        else
          {
            ignoreFinished = true;
            sb.append(getJavaOperator(ce.operator));
            sb.append(fixForRuleBasedCollator(ce.code));
          }
      }
    return sb.toString();
  }
}
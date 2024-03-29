import java.lang.reflect.*;

public class Query
{
    private class Operator
    {
        public static final int NONE = 0;
        public static final int GREATER_OR_EQUAL = 1;
        public static final int LESS_OR_EQUAL = 2;
        public static final int GREATER_THAN = 3;
        public static final int LESS_THAN = 4;
        public static final int EQUAL = 5;
        public static final int NOT_EQUAL = 6;
    }

    private static Query parseInternal(String query, boolean assign)
        throws QuerySyntaxException
    {
        String[] split = Endpoint.splitCommand(query);

        final String[] validOperators = new String[] {
            null, // 0
            ">=", // 1
            "<=", // 2
            ">",  // 3
            "<",  // 4
            "==", // 5
            "!="  // 6
        };

        Query rootCondition = new Query();
        Query condition = rootCondition;

        int i = 0;
        while (condition != null)
        {
            if (i >= split.length) {
                throw new QuerySyntaxException("expected a field name", split, i);
            }

            if (!assign && (split[i].equals("true") || split[i].equals("false"))) {
                condition.isConstant = true;
                condition.constant = (Boolean) split[i].equals("true");
            } else {
                try {
                    condition.field = Student.class.getDeclaredField(split[i]);
                } catch (Exception e) {
                    throw new QuerySyntaxException("invalid field name", split, i);
                }

                if (++i >= split.length) {
                    throw new QuerySyntaxException("expected an operator", split, i);
                }

                if (assign) {
                    if (split[i].equals("=")) {
                        condition.operator = Operator.EQUAL;
                    }
                } else {
                    for(int o = 0; o < validOperators.length; ++ o) {
                        if (split[i].equals(validOperators[o])) {
                            condition.operator = o;
                            break;
                        }
                    }
                }

                if (condition.operator == Operator.NONE) {
                    throw new QuerySyntaxException("invalid operator", split, i);
                }

                if (++i >= split.length) {
                    throw new QuerySyntaxException("expected a constant value", split, i);
                }

                try {
                    condition.constant = Integer.parseInt(split[i]);
                } catch (Exception e) {
                    condition.constant = split[i];
                }
            }

            if (++i < split.length) {
                if (!assign && !split[i].equals("or") && !split[i].equals("and")) {
                    throw new QuerySyntaxException("expected either \"or\" or \"and\" between clauses", split, i);
                }

                condition.next = new Query();
                condition.isDisjunction = !assign && split[i++].equals("or");
            }

            condition = condition.next;
        }

        return rootCondition;
    }

    public static Query parseQuery(String query)
        throws QuerySyntaxException
    {
        return parseInternal(query, false);
    }

    public static Query parseAssignment(String query)
        throws QuerySyntaxException
    {
        return parseInternal(query, true);
    }

    public Field field;
    public int operator;
    public Object constant;
    public Query next;
    public boolean isDisjunction;
    public boolean isConstant;

    public Query()
    {
        field = null;
        operator = Operator.NONE;
        constant = null;
        next = null;

        isDisjunction = false;
        isConstant = false;
    }

    private boolean evaluate(String a, String b)
    {
        switch (operator) {
            case Operator.GREATER_OR_EQUAL:
                return a.compareTo(b) >= 0;
            case Operator.LESS_OR_EQUAL:
                return a.compareTo(b) <= 0;
            case Operator.GREATER_THAN:
                return a.compareTo(b) > 0;
            case Operator.LESS_THAN:
                return a.compareTo(b) < 0;
            case Operator.EQUAL:
                return a.equals(b);
            case Operator.NOT_EQUAL:
                return !a.equals(b);
            default:
                return false;
        }
    }

    private boolean evaluate(Integer a, Integer b)
    {
        switch (operator) {
            case Operator.GREATER_OR_EQUAL:
                return a >= b;
            case Operator.LESS_OR_EQUAL:
                return a <= b;
            case Operator.GREATER_THAN:
                return a > b;
            case Operator.LESS_THAN:
                return a < b;
            case Operator.EQUAL:
                return a == b;
            case Operator.NOT_EQUAL:
                return a != b;
            default:
                return false;
        }
    }

    private boolean carry(Student student)
    {
        if (next == null) return false;
        if (isDisjunction) return next.evaluate(student);
        return next.carry(student);
    }

    public boolean evaluate(Student student)
    {
        boolean thisEval = (isConstant && (Boolean) constant);

        if (!isConstant) {
            Object val;
            try {
                val = field.get(student);

                boolean wasInteger = false;
                if (constant instanceof Integer) {
                    if (field.getType() == Department.class) {
                        val = ((Department) val).identifier;
                        thisEval = evaluate((Integer) val, (Integer) constant);
                        wasInteger = true;
                    } else {
                        try {
                            thisEval = evaluate(Integer.parseInt(val.toString()), (Integer) constant);
                            wasInteger = true;
                        } catch (Exception e) { }
                    }
                }

                if (!wasInteger) {
                    if (field.getType() == Department.class) {
                        val = ((Department) val).name;
                    }

                    thisEval = evaluate(val.toString(), constant.toString());
                }
            } catch (Exception e) { }
        }

        if (next == null) return thisEval;

        if (isDisjunction) {
            return thisEval || next.evaluate(student);
        } else {
            return (thisEval && next.evaluate(student)) || carry(student);
        }
    }

    public void assign(Endpoint endpoint, Student student)
    {
        try {
            if (field.getType() == Department.class) {
                if (constant instanceof Integer) {
                    field.set(student, endpoint.getDepartment((Integer) constant));
                } else {
                    field.set(student, endpoint.getDepartment(constant.toString()));
                }
            } else if (field.get(student) instanceof Integer) {
                if (constant instanceof Integer) {
                    field.set(student, constant);
                } else {
                    field.set(student, 0);
                }
            } else {
                field.set(student, constant.toString());
            }
        } catch (Exception e) { }

        if (next != null) {
            next.assign(endpoint, student);
        }
    }
}

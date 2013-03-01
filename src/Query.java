import java.lang.reflect.*;

public class Query
{
    private enum Token
    {
        Constant,
        FieldAccess,
        NotOperator,
        DisjunctionOperator,
        ConjunctionOperator,
        EqualityOperator
    }

    public class Select
        extends Query
    {
        public static IPredicate parsePredicate(String str)
        {
            
        }
    }

    public interface IPredicate
    {
        boolean evaluate(Student student);
    }

    public interface IValue
    {
        Object evaluate(Student student);
    }

    public class Constant
        implements IValue
    {
        public final Object value;

        public Constant(Object value)
        {
            this.value = value;
        }

        @Override
        public Object evaluate(Student student)
        {
            return value;
        }
    }

    public class FieldAccess
        implements IValue
    {
        public final Field field;

        public FieldAccess(String fieldName)
            throws NoSuchFieldException
        {
            field = Student.class.getDeclaredField(fieldName);
        }

        @Override
        public Object evaluate(Student student)
        {
            try {
                Object value = field.get(student);

                if (value instanceof Department) {
                    return ((Department) value).name;
                }

                return value;
            } catch (IllegalAccessException e) {
                return null;
            }
        }
    }

    public class NotOperator
        implements IPredicate
    {
        public final IPredicate predicate;

        public NotOperator(IPredicate predicate)
        {
            this.predicate = predicate;
        }

        @Override
        public boolean evaluate(Student student)
        {
            return !predicate.evaluate(student);
        }
    }

    public abstract class BooleanOperator
        implements IPredicate
    {
        public final IPredicate left;
        public final IPredicate right;

        public BooleanOperator(IPredicate left, IPredicate right)
        {
            this.left = left;
            this.right = right;
        }
    }

    public class DisjunctionOperator
        extends BooleanOperator
    {
        public DisjunctionOperator(IPredicate left, IPredicate right)
        {
            super(left, right);
        }

        @Override
        public boolean evaluate(Student student)
        {
            return left.evaluate(student) || right.evaluate(student);
        }
    }

    public class ConjunctionOperator
        extends BooleanOperator
    {
        public ConjunctionOperator(IPredicate left, IPredicate right)
        {
            super(left, right);
        }

        @Override
        public boolean evaluate(Student student)
        {
            return left.evaluate(student) && right.evaluate(student);
        }
    }

    public abstract class ComparisonOperator
        implements IPredicate
    {
        public final IValue left;
        public final IValue right;

        public ComparisonOperator(IValue left, IValue right)
        {
            this.left = left;
            this.right = right;
        }
    }

    public class EqualityOperator
        extends ComparisonOperator
    {
        public EqualityOperator(IValue left, IValue right)
        {
            super(left, right);
        }

        @Override
        public boolean evaluate(Student student)
        {
            Object lobj = left.evaluate(student);
            Object robj = right.evaluate(student);

            if (lobj instanceof Integer && robj instanceof Integer) {
                return (Integer) lobj == (Integer) robj;
            } else if (lobj instanceof Integer || robj instanceof Integer) {
                if (robj instanceof Integer) {
                    Object temp = lobj;
                    lobj = robj;
                    robj = temp;
                }

                try {
                    robj = Integer.parseInt(robj.toString());
                } catch (Exception e) {
                    return false;
                }

                return (Integer) lobj == (Integer) robj;
            } else {
                return lobj.equals(robj);
            }
        }
    }
}

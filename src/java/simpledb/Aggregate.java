package simpledb;

import java.util.*;

/**
 * The Aggregation operator that computes an aggregate (e.g., sum, avg, max,
 * min). Note that we only support aggregates over a single column, grouped by a
 * single column.
 */
public class Aggregate extends Operator {

    private static final long serialVersionUID = 1L;
    private OpIterator child;
    private final int afield;
    private final int gfield;
    private final Aggregator.Op aop;
    private final TupleDesc td;
    private final Aggregator aggregator; // the aggregator
    private OpIterator aggregatedIterator = null; // iterator of the aggregated tuples

    /**
     * Constructor.
     * 
     * Implementation hint: depending on the type of afield, you will want to
     * construct an {@link IntegerAggregator} or {@link StringAggregator} to help
     * you with your implementation of readNext().
     * 
     * 
     * @param child
     *            The OpIterator that is feeding us tuples.
     * @param afield
     *            The column over which we are computing an aggregate.
     * @param gfield
     *            The column over which we are grouping the result, or -1 if
     *            there is no grouping
     * @param aop
     *            The aggregation operator to use
     */
    public Aggregate(OpIterator child, int afield, int gfield, Aggregator.Op aop) {
        this.child = child;
        this.afield = afield;
        this.gfield = gfield;
        this.aop = aop;
        this.td = child.getTupleDesc();
        final Type gtype = (gfield == Aggregator.NO_GROUPING)? null : td.getFieldType(gfield);
        if (td.getFieldType(afield) == Type.INT_TYPE) {
            aggregator = new IntegerAggregator(gfield, gtype, afield, aop);
        } else if (td.getFieldType(afield) == Type.STRING_TYPE) {
            aggregator = new StringAggregator(gfield, gtype, afield, aop);
        } else {
            throw new IllegalArgumentException("Impossible to reach here");
        }
    }
    
    /**
     * Loop over the child OpIterator and aggregate the fields
     * @throws TransactionAbortedException if transcation aborted
     * @throws DbException when there are problems opening/accessing the database.
     * @throws NoSuchElementException if there are no more tuples when iterating
     */
    private void aggregateAll() throws NoSuchElementException, DbException, TransactionAbortedException {
        child.open();
        while(child.hasNext()) {
            aggregator.mergeTupleIntoGroup(child.next());
        }
        child.close();
    }

    /**
     * @return If this aggregate is accompanied by a groupby, return the groupby
     *         field index in the <b>INPUT</b> tuples. If not, return
     *         {@link simpledb.Aggregator#NO_GROUPING}
     * */
    public int groupField() {
        return gfield;
    }

    /**
     * @return If this aggregate is accompanied by a group by, return the name
     *         of the groupby field in the <b>OUTPUT</b> tuples. If not, return
     *         null;
     * */
    public String groupFieldName() {
        if (gfield == Aggregator.NO_GROUPING) {
            return null;
        } else {
            return td.getFieldName(gfield);
        }
    }

    /**
     * @return the aggregate field
     * */
    public int aggregateField() {
        return afield;
    }

    /**
     * @return return the name of the aggregate field in the <b>OUTPUT</b>
     *         tuples
     * */
    public String aggregateFieldName() {
        return td.getFieldName(afield);
    }

    /**
     * @return return the aggregate operator
     * */
    public Aggregator.Op aggregateOp() {
        return aop;
    }

    public static String nameOfAggregatorOp(Aggregator.Op aop) {
        return aop.toString();
    }

    public void open() throws NoSuchElementException, DbException,
	    TransactionAbortedException {
        super.open();
        aggregateAll();
        aggregatedIterator = aggregator.iterator();
        aggregatedIterator.open();
    }

    /**
     * Returns the next tuple. If there is a group by field, then the first
     * field is the field by which we are grouping, and the second field is the
     * result of computing the aggregate. If there is no group by field, then
     * the result tuple should contain one field representing the result of the
     * aggregate. Should return null if there are no more tuples.
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        if (aggregatedIterator.hasNext()){
            return aggregatedIterator.next();
        } else {
            return null;
        }
    }

    public void rewind() throws DbException, TransactionAbortedException {
        aggregatedIterator = aggregator.iterator();
        aggregatedIterator.open();
    }

    /**
     * Returns the TupleDesc of this Aggregate. If there is no group by field,
     * this will have one field - the aggregate column. If there is a group by
     * field, the first field will be the group by field, and the second will be
     * the aggregate value column.
     * 
     * The name of an aggregate column should be informative. For example:
     * "aggName(aop) (child_td.getFieldName(afield))" where aop and afield are
     * given in the constructor, and child_td is the TupleDesc of the child
     * iterator.
     */
    public TupleDesc getTupleDesc() {
        if (gfield != Aggregator.NO_GROUPING){
            return new TupleDesc(new Type[]{td.getFieldType(gfield), Type.INT_TYPE},
                    new String[]{groupFieldName(), aop.toString() + "(" + aggregateFieldName() + ")"});
        } else {
            return new TupleDesc(new Type[]{Type.INT_TYPE}, new String[]{aop.toString() + "(" + aggregateFieldName() + ")"});
        }
    }

    public void close() {
        super.close();
        aggregatedIterator = null;
    }

    @Override
    public OpIterator[] getChildren() {
        return new OpIterator[]{child};
    }

    @Override
    public void setChildren(OpIterator[] children) {
        assert children.length == 1 : "Incorrent length of OpIterator[]!";
        child = children[0];
    }
    
}

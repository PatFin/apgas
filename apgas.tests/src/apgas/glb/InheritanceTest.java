/**
 *
 */
package apgas.glb;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import apgas.glb.example.Sum;

/**
 * @author Patrick Finnerty
 *
 */
@RunWith(Parameterized.class)
public class InheritanceTest {

  /** Computation ressource used for the test */
  GLBProcessor processor;

  /**
   * Constructor
   *
   * @param p
   *          instance to be tested
   */
  public InheritanceTest(GLBProcessor p) {

    processor = p;
  }

  /**
   * Tests a simple inheritance situation First will spawn an AMOUNT number of
   * Second instances which in turn are going to spawn the same amount of Sum(1)
   * instances. Hence the final expected result of Sum is AMOUNT.
   */
  @Test(timeout = 5000)
  public void inheritanceTest1() {
    final int RESULT = 400;
    final First bag = new First(RESULT);

    final Sum s = processor.compute(bag, new Sum(0));
    assert s != null;
    assertEquals(RESULT, s.sum);
  }

  /**
   * Yields the {@link GLBProcessor} implementations to be tested.
   *
   * @return collection of GLBProcessor
   */
  @Parameterized.Parameters
  public static Collection<Object[]> toTest() {
    final Collection<Object[]> toReturn = new ArrayList<>();
    final GLBProcessor a = GLBProcessorFactory.LoopGLBProcessor(500, 1);
    final Object[] first = { a };
    toReturn.add(first);
    final GLBProcessor b = GLBProcessorFactory.GLBProcessor(500, 1,
        new HypercubeStrategy());
    final Object[] second = { b };
    toReturn.add(second);
    return toReturn;
  }

  /**
   * Parent class for {@link First} and {@link Second}
   *
   * @author Patrick Finnerty
   *
   */
  @SuppressWarnings("serial")
  private abstract class AbstractBag
      implements Bag<AbstractBag, Sum>, Serializable {

    /** Data shared amongst children */
    public int data;

    /**
     * Constructor
     * <p>
     * Supplies the initial value for the data.
     *
     * @param d
     *          initial value for data.
     */
    protected AbstractBag(int d) {
      data = d;
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#merge(apgas.glb.Bag)
     */
    @Override
    public void merge(AbstractBag b) {
      data += b.data;
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#isEmpty()
     */
    @Override
    public boolean isEmpty() {
      return data <= 0;
    }

    @Override
    public void submit(Sum r) {
      // no action
    }

  }

  /**
   * Spawns {@link Second} instances as specified by the number given in the
   * constructor.
   *
   * @author Patrick Finnerty
   *
   */
  @SuppressWarnings("serial")
  private class First extends AbstractBag {

    /**
     * Construcotr
     *
     * @param d
     *          initial data value
     */
    protected First(int d) {
      super(d);
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#process(int)
     */
    @Override
    public void process(int workAmount, WorkCollector<Sum> p) {
      while (data > 0 && workAmount > 0) {
        p.giveBag(new Second(1));
        data--;
        workAmount--;
      }
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#split()
     */
    @Override
    public AbstractBag split() {
      final int split = data / 2;
      data -= split;
      return new First(split);
    }
  }

  private class Second extends AbstractBag {

    /**
     *
     */
    private static final long serialVersionUID = -6762267257258758078L;
    int result = 0;

    public Second(int i) {
      super(i);
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#split()
     */
    @Override
    public AbstractBag split() {
      final int split = data / 2;
      data -= split;
      return new Second(split);
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#process(int)
     */
    @Override
    public void process(int workAmount, WorkCollector<Sum> p) {
      while (data > 0 && workAmount > 0) {
        result++;
        data--;
        workAmount--;
      }
    }

    @Override
    public void submit(Sum r) {
      r.sum += result;
    }
  }
}

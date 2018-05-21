/**
 *
 */
package apgas.glb;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Computing service abstraction. GLBProcessor uses a lifeline-based global load
 * balancing algorithm.
 * <p>
 * The work that can be handled by the GLBProcessor is a {@link Bag}. Initial
 * {@link Bag}s for your computation can be added to this instance by calling
 * {@link #addBag(Bag)}. Note that several {@link Bag}s can be given to the
 * GLBProcessor, as long as they all produce the type of {@link Fold} handled by
 * your instance.
 * <p>
 * Computation is launched using the {@link #compute()} method. The result can
 * then be obtained using the {@link #result()} method.
 * <p>
 * If one wishes to use same computing instance for several successive
 * computations, method {@link #reset()} should be called before adding the new
 * {@link Bag}s in order to avoid existing results from previous computations to
 * get mixed with the new ones.
 *
 * @author Patrick Finnerty
 *
 */
public interface GLBProcessor<R extends Fold<R> & Serializable> {

  /**
   * Launches the computation of the work given to the GLBProcessor and returns
   * the result.
   */
  public <B extends Bag<B, R> & Serializable, S extends Supplier<R> & Serializable> R compute(
      B bag, S initializer);

  /**
   * Launches the computation of the work given to the GLBProcessor and return
   * the result.
   * 
   * @param bags
   *          collection of {@link Bag} to be processed
   * @param initializer
   *          the initializer function for the {@link Fold} instance
   * @return computation result
   */
  public <B extends Bag<B, R> & Serializable, S extends Supplier<R> & Serializable> R compute(
      Collection<B> bags, S initializer);
}

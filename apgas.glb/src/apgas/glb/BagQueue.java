/**
 *
 */
package apgas.glb;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Patrick Finnerty
 *
 */
class BagQueue<R extends Result<R> & Serializable> implements WorkCollector<R> {

  /** Index of the last Bag that actually had some work in it */
  private int lastPlaceWithWork = 0;

  /** First free index in the bag queue */
  private int last = 0;

  /** Array used as circular buffer to contain {@link Bag}s */
  private Object bags[] = new Bag[16];

  /**
   * Doubles the capacity of the {@link #bags} array. Copying the contained
   * {@link Bag}s to the new array.
   */
  private void grow() {
    bags = Arrays.copyOf(bags, bags.length * 2);
  }

  /**
   * Adds the {@link Bag} given as parameter to the end of the {@link BagQueue}.
   * If another instance of the same class as the given parameter exists in the
   * {@link BagQueue}, it will merged into it.
   *
   * @param <B>
   *          parameter type
   * @param b
   *          the bag to add to the queue
   */
  private <B extends Bag<B, R> & Serializable> void addBag(B b) {
    for (int i = 0; i < last; i++) {
      @SuppressWarnings("unchecked")
      final B a = (B) bags[i];
      if (b.getClass().getName().equals(a.getClass().getName())) {

        a.merge(b);
        return;
      }
    }
    // No existing instance of the same class in the queue, we add the work at
    // the end
    bags[last] = b;
    last++;
    if (last == bags.length) {
      grow();
    }

  }

  /**
   * Removes all {@link Bag}s from the {@link BagQueue}. The {@link BagQueue} is
   * empty when this method returns.
   */
  public void clear() {
    for (int i = 0; i < bags.length; i++) {
      bags[i] = null;
    }
    last = 0;
    lastPlaceWithWork = 0;
  }

  /**
   * Indicates if this {@link TaskBag} is empty, that is that it does not
   * contain any {@link Bag}.
   *
   * @return true if empty, false otherwise
   */
  @SuppressWarnings("rawtypes")
  public boolean isEmpty() {

    if (last == 0) {
      return true;
    }
    final int i = lastPlaceWithWork;
    Bag b = (Bag) bags[lastPlaceWithWork];
    if (b.isEmpty()) {
      do {
        lastPlaceWithWork = (lastPlaceWithWork + 1) % last;
        b = (Bag) bags[lastPlaceWithWork];
        if (!b.isEmpty()) {
          return false;
        }
      } while (i != lastPlaceWithWork);
      return true;
    }
    return false;

  }

  /**
   * Processes the given amount of work in the first Bag in the queue.
   *
   * @param workAmount
   *          amount of work to be processed
   */
  @SuppressWarnings("rawtypes")
  public void process(int workAmount) {

    final int i = lastPlaceWithWork;
    Bag b = (Bag) bags[i];
    if (b.isEmpty()) {
      do {
        lastPlaceWithWork = (lastPlaceWithWork + 1) % last;
        b = (Bag) bags[i];
      } while (b.isEmpty() && i != lastPlaceWithWork);
    }
    b.process(workAmount);

  }

  /**
   * Yields back the result from the bags contained by this bag queue.
   *
   * @return <R> instance, possibly null.
   */
  @SuppressWarnings("unchecked")
  public R result() {
    R res = null;
    int i = 0;
    while (res == null && i < last) {
      res = ((Bag<?, R>) bags[i]).submit();
      i++;
    }
    while (i < last) {
      final R toFold = ((Bag<?, R>) bags[i]).submit();
      if (toFold != null) {
        res.fold(((Bag<?, R>) bags[i]).submit());
      }
      i++;
    }
    return res;
  }

  /**
   * Tries to split the {@link Bag}s contained in the {@link BagQueue} and
   * returns the split. If no work could be be split from the any {@link Bag},
   * returns null.
   *
   * @param <B>
   *          type parameter
   * @return some {@link Bag} instance or null is no split could be performed.
   */
  @SuppressWarnings("unchecked")
  public <B extends Bag<B, R> & Serializable> B split() {

    B split = null;
    for (int i = 0; i != last; i++) {
      split = ((B) bags[i]).split();
      if (split != null) {
        break;
      }
    }
    return split;

  }

  /**
   * Constructor
   */
  public BagQueue() {
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.WorkCollector#giveBag(apgas.glb.Bag)
   */
  @Override
  public <B extends Bag<B, R> & Serializable> void giveBag(B b) {
    b.setWorkCollector(this);
    addBag(b);
  }

}

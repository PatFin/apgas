/**
 *
 */
package apgas.glb.example;

import java.io.Serializable;

import apgas.glb.Fold;

/**
 * Performs the addition on integers.
 *
 * @author Patrick Finnerty
 *
 */
public class Sum implements Fold<Sum>, Serializable {

  /**
   * Serial Version UID
   */
  private static final long serialVersionUID = 3582168956043482749L;

  /** Integer in which the sum is performed */
  public long sum;

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.Fold#fold(apgas.glb.Fold)
   */
  @Override
  public void fold(Sum f) {
    sum += f.sum;
  }

  /*
   * (non-Javadoc)
   * 
   * @see apgas.glb.Fold#id()
   */
  @Override
  public String id() {
    return "";
  }

  /**
   * Construtor
   *
   * @param s
   *          the initial value of the sum
   */
  public Sum(int s) {
    sum = s;
  }

}

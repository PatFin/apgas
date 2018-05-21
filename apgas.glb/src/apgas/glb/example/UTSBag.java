/**
 *
 */
package apgas.glb.example;

import java.io.Serializable;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import apgas.glb.Bag;
import apgas.glb.GLBProcessor;
import apgas.glb.GLBProcessorFactory;
import apgas.glb.HypercubeStrategy;
import apgas.glb.WorkCollector;

/**
 * {@link Bag} implementation of an Unbalanced Tree Search computation.
 * <p>
 * This class is an adapatation from the apgas.examples.UTS class to fit the
 * {@link apgas.glb.GLBProcessor} interface. The result returned by
 * {@link UTSBag} is the number of nodes explored, using the {@link Sum} class.
 *
 * @author Patrick Finnerty
 *
 */
public class UTSBag implements Serializable, Bag<UTSBag, Sum> {

  /** Serial Version UID */
  private static final long serialVersionUID = 2200935927036145803L;

  /** Branching factor: 4 */
  private static final double den = Math.log(4.0 / (1.0 + 4.0));

  /**
   * Returns a messageDigest instance that will create a seemingly random output
   * for some input.
   *
   * @return SHA-1 message digest if available, throws an exception if not
   *         available.
   */
  public static MessageDigest encoder() {
    try {
      return MessageDigest.getInstance("SHA-1");
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  /** Array containing the seed for each node's subtree */
  byte[] hash;

  /** Keeps track of the tree level corresponding to the index */
  int[] depth;

  /**
   * Lower bound of the number of nodes left to explore at this level of this
   * search
   */
  int[] lower;

  /**
   * Upper bound of the number of nodes left to explore at this level this
   * search
   */
  int[] upper;

  /** Number of nodes in the bag */
  public int size = 0;

  private int count = 0;

  /**
   * Constructor
   * <p>
   * Creates an empty UTS with no nodes to explore.
   */
  public UTSBag() {
  }

  public UTSBag(int n) {
    hash = new byte[n * 20 + 4]; // slack for in-place SHA1 computation : n*20
                                 // as 20 cells are needed for each level and 4
                                 // extra for last cell child seeds
    depth = new int[n];
    lower = new int[n];
    upper = new int[n];
  }

  /**
   * Generates the next level of nodes from the current level (kept by member
   * size) and stores it in the arrays depth, lower and upper.
   *
   * @param md
   *          pseudo-random sequence generator
   * @param d
   *          depth limit of desired tree. If 0, will increase generated
   * @throws DigestException
   *           if there is an issue with the encoder
   */
  private void digest(MessageDigest md, int d) throws DigestException {
    // Creates more space in the arrays if need be
    if (size >= depth.length) {
      grow();
    }
    // processor.giveFold(new Sum(1));
    ++count; // We are exploring one node (expanding its child nodes)
    final int offset = size * 20;
    md.digest(hash, offset, 20); // Writes onto array hash on the next 20
    // cells or bytes.

    // What is the tree going to look like ?
    final int v = ((0x7f & hash[offset + 16]) << 24)
        | ((0xff & hash[offset + 17]) << 16) | ((0xff & hash[offset + 18]) << 8)
        | (0xff & hash[offset + 19]); // v is the positive integer made of the 4
                                      // bytes in the hash array generated by
                                      // the message digest 'digest' previous
                                      // call
    final int n = (int) (Math.log(1.0 - v / 2147483648.0) / den);
    // 2.147.483.648 is written as 1 followed by 63 zeros in binary : -1.
    // v / 2.147.483.648 is then in the range (-2147483647,0]
    // n is then a positive integer, sometimes = 0, sometimes greater.
    if (n > 0) {
      if (d > 1) { // Bound for tree depth
        // We create node size
        depth[size] = d - 1;
        lower[size] = 0;
        upper[size] = n;
        size++;
      } else {
        // processor.giveFold(new Sum(n));
        count += n;
      }
    }
  }

  /**
   * Initialises the hash array with zeros from indeces 0 to 15 and writes
   * number s (32 bit - 4 bytes integer) in the next 4 cells (from indeces 16 to
   * 19 included), updates the message digest to use the beginning of array hash
   * on these cells and calls #{@link UTSBag#digest(MessageDigest, int)}.
   *
   * @param md
   *          the message digest used to generate a 'random' sequence
   * @param seed
   *          the seed to generate the tree from
   * @param depth
   *          the depth bound of the tree to create
   * @see #digest(MessageDigest, int)
   */
  public void seed(MessageDigest md, int seed, int depth) {
    try {
      for (int i = 0; i < 16; ++i) {
        hash[i] = 0;
      }
      hash[16] = (byte) (seed >> 24);
      hash[17] = (byte) (seed >> 16);
      hash[18] = (byte) (seed >> 8);
      hash[19] = (byte) seed;
      md.update(hash, 0, 20);
      digest(md, depth);
    } catch (final DigestException e) {
    }
  }

  /**
   * Explores the current node, computing the arrays[size] cells. The main point
   * is to determine the number of child nodes of the current node
   *
   * @param md
   *          the hash digest
   * @throws DigestException
   *           thrown by {@link #digest(MessageDigest, int)}
   */
  public void expand(MessageDigest md) throws DigestException {
    final int top = size - 1; // top is the previous node, top is then the index
                              // of the parent node in the arrays
    final int d = depth[top];
    final int l = lower[top];
    final int u = upper[top] - 1;
    if (u == l) {
      size = top; // We go back to the top node, we have explored all nodes on
                  // the top + 1 level
    } else {
      upper[top] = u; // We decrement the child nodes of top (the current node's
                      // parent node) : we have finished exploring all the child
                      // nodes of the current node
    }

    // Setting up a new 'seed' to explore the current node's child nodes
    final int offset = top * 20;
    hash[offset + 20] = (byte) (u >> 24);
    hash[offset + 21] = (byte) (u >> 16);
    hash[offset + 22] = (byte) (u >> 8);
    hash[offset + 23] = (byte) u;
    md.update(hash, offset, 24); // seed takes into account both the parent seed
                                 // and 'u'
    digest(md, d);
  }

  /**
   * Explores the whole tree
   *
   * @param md
   *          the message digest used to generate the tree from the seed
   */
  public void run(MessageDigest md) {
    try {
      while (!isEmpty()) {
        expand(md);
      }
    } catch (final DigestException e) {
    }
  }

  /**
   * Creates a new UTS instance containing half of the nodes left to explore
   * from the current state of affairs
   *
   * @return a new UTS instance with half the known work of the current one
   */
  @Override
  public UTSBag split() {
    int s = 0;
    for (int i = 0; i < size; ++i) {
      if ((upper[i] - lower[i]) >= 2) {
        ++s;
      }
    }
    if (s == 0) {
      return null;
    }
    final UTSBag b = new UTSBag(s);
    for (int i = 0; i < size; ++i) {
      final int p = upper[i] - lower[i];
      if (p >= 2) {
        System.arraycopy(hash, i * 20, b.hash, b.size * 20, 20);
        b.depth[b.size] = depth[i];
        b.upper[b.size] = upper[i];
        b.lower[b.size++] = upper[i] -= p / 2;
      }
    }
    return b;
  }

  /**
   * Merges the given UTS's work into the current instance
   *
   * @param b
   *          the work to be done
   */
  @Override
  public void merge(UTSBag b) {
    final int s = size + b.size;
    while (s > depth.length) {
      grow();
    }
    System.arraycopy(b.hash, 0, hash, size * 20, b.size * 20);
    System.arraycopy(b.depth, 0, depth, size, b.size);
    System.arraycopy(b.lower, 0, lower, size, b.size);
    System.arraycopy(b.upper, 0, upper, size, b.size);
    size = s;
  }

  /**
   * Increases the size of array members hash, depth, lower and upper.
   */
  private void grow() {
    final int n = depth.length * 2;
    final byte[] h = new byte[n * 20 + 4];
    final int[] d = new int[n];
    final int[] l = new int[n];
    final int[] u = new int[n];
    System.arraycopy(hash, 0, h, 0, size * 20);
    System.arraycopy(depth, 0, d, 0, size);
    System.arraycopy(lower, 0, l, 0, size);
    System.arraycopy(upper, 0, u, 0, size);
    hash = h;
    depth = d;
    lower = l;
    upper = u;
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.Bag#process(int)
   */
  @Override
  public void process(int workAmount) {
    while (!isEmpty() && workAmount > 0) {
      try {
        expand(encoder());
      } catch (final DigestException e) {
        e.printStackTrace();
      }
      workAmount--;
    }

  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.Bag#submit(apgas.glb.Fold)
   */
  @Override
  public void submit(Sum r) {
    r.sum += count;
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.Bag#isEmpty()
   */
  @Override
  public boolean isEmpty() {
    return size < 1;
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.Bag#setProcessor(apgas.glb.WorkCollector)
   */
  @Override
  public void setWorkCollector(WorkCollector<Sum> p) {
  } // Not used

  public static String sub(String str, int start, int end) {
    return str.substring(start, Math.min(end, str.length()));
  }

  /**
   * Launches the computation
   *
   * @param args
   *          one argument can be specified : the depth of the tree to explore.
   *          If no argument is given, the default value, 13 is used.
   */
  public static void main(String[] args) {
    int depth = 13;
    try {
      depth = Integer.parseInt(args[0]);
    } catch (final Exception e) {
    }

    final MessageDigest md = encoder();

    final GLBProcessor<Sum> processor = GLBProcessorFactory.GLBProcessor(500, 1,
        new HypercubeStrategy());

    final UTSBag taskBag = new UTSBag(64);
    taskBag.seed(md, 19, depth - 2);

    System.out.println("Warmup...");

    processor.compute(taskBag, () -> new Sum(0));

    final UTSBag secondBag = new UTSBag(64);
    secondBag.seed(md, 19, depth);

    System.out.println("Starting...");
    final long start = System.nanoTime();
    final long count = processor.compute(secondBag, () -> new Sum(0)).sum;

    System.out.println("Finished.");

    final long finish = System.nanoTime();

    final long computationTime = finish - start;

    System.out.println("Depth: " + depth + ", Performance: " + count + "/"
        + sub("" + computationTime / 1e9, 0, 6) + " = "
        + sub("" + (count / (computationTime / 1e3)), 0, 6) + "M nodes/s");
  }
}

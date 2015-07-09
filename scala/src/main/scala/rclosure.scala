package rclosure

import scala.collection.immutable.Map

/**
 * Describes a resource closure
 * @tparam STATE
 * @tparam V
 */
trait RClosure[STATE, V] {

  /**
   * Init
   * @return
   */
  def apply(): STATE

  /**
   * Run
   * @param state
   * @param v
   * @return
   */
  def apply(state: STATE, v: V): STATE

  /**
   * Close
   * @param closeMap
   */
  def apply(closeMap: Map[String, Any]): Unit

}

/**
 * Describes a resource closure generator
 * @tparam ENV The environment passed in
 * @tparam STATE The state that the
 * @tparam V The value passed in to the resource closure as apply(state, v)
 */
trait RClosureGen[ENV, STATE, V] {
  def apply(env: ENV): RClosure[STATE, V]
}

/**
 * Describes a resource closure factory that returns resource closure generators
 */
trait RClosureFactory {
  def apply[ENV, STATE, V](f: RClosureGen[ENV, STATE, V]): RClosureGen[ENV, STATE, V]
}


object RC {
  import scala.collection.immutable.HashMap
  val EMPTY_MAP = new HashMap[String, Any]();

  /**
   * Run the rClosureGen once by applying <br/>
   * <pre>
   * rc = rcg.apply(env)
   * try rc.apply(rc.apply(), Nil) finally rc.apply(HashMap)
   * </pre>
   *
   * @param env
   * @param rcg
   * @tparam ENV
   * @tparam STATE
   * @tparam V
   * @return The value returned by rClosureGen.apply(state, v)
   */
  def runOnce[ENV, STATE, V](env: ENV, rcg: RClosureGen[ENV, STATE, V]): STATE = {
    val rc = rcg.apply(env)
    try
      rc.apply(rc.apply(), Nil.asInstanceOf[V])
    finally
      rc.apply(EMPTY_MAP)
  }

  /**
   * Compose one resource closure factory and resource closure gen
   * @param f1
   * @param f2
   * @tparam ENV
   * @tparam STATE
   * @tparam V
   * @return
   */
  def rcompose[ENV, STATE, V](f2: RClosureGen[ENV, STATE, V], f1: RClosureFactory) = f1.apply[ENV, STATE, V](f2)

  /**
   * Compose a list of resource closure factories with a last resource closure gen
   * @param fs
   * @param f
   * @tparam ENV
   * @tparam STATE
   * @tparam V
   * @return resource closure gen
   */
  def rcompose[ENV, STATE, V](f: RClosureGen[ENV, STATE, V], fs: RClosureFactory*) = {
    if (fs.isEmpty)
      f
    else
      fs.reverse.foldLeft(f)((rcg: RClosureGen[ENV, STATE, V], rcf: RClosureFactory) => rcf.apply(rcg))
  }

}

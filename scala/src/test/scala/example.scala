package example

import rclosure._
import scalikejdbc._
import scala.collection.immutable.Map
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter

object Example {


  /**
   * A resource closure gen, that will write data to a file and return the
   * write record count
   * @return
   */
  def writeToFile = new RClosureGen[Map[String, Any], Int, String] {
    def apply(env:Map[String, Any]) = {

      val writer = new BufferedWriter(new FileWriter(new File(getFileName(env))))

      new RClosure[Int, String] {
        def apply() = { 0 }   //init value
        def apply(n:Int, name:String) = { //write data
          writer.append(name)
          writer.newLine()
          n + 1
        }
        def apply(closeMap: Map[String, Any]) = { writer.close()} //close file
      }
    }
  }

  def getFileName(env: Map[String, Any]): String = {
    env.get("file") match {
      case Some(f:String) => f
      case _ => throw new RuntimeException("file property not found")
    }
  }
  /**
   * A resource closure factory that will return a resource closure gen that will open a db connection, <br/>
   * query data and send it to the<br/>
   * apply(state, record) function of the rcg resource closure gen passed in<br/>
   * Is meant to be run using run-once
   */
  def readFromDB[ENV] = new RClosureFactory {
    def apply[ENV, RET, V](rcg:RClosureGen[ENV, RET, V]) = {
      readFromDbHelper[ENV, RET, V](rcg)
    }
  }

  /**
   * Helper function, please use readFromDB
   * A resource closure gen that will open a db connection, query data and send it to the<br/>
   * apply(state, record) function of the rcg resource closure gen passed in<br/>
   */
  private def readFromDbHelper[ENV, RET, V](rcg:RClosureGen[ENV, RET, V]): RClosureGen[ENV, RET, V] = {

    new RClosureGen[ENV, RET, V]{
      def apply(env: ENV) = {

        //open db connection
        val conn = ConnectionPool.borrow()

        val rc = rcg.apply(env)
        val init:RET = rc.apply()

        new RClosure [RET, V]{

          def apply() = { init } //return init of rcg

          def apply(state: RET, v: V): RET = { //run once, V is ignored, and so is state.
            var state:RET = init

            DB readOnly { implicit session =>
              sql"select * from test".foreach { rs =>
                state = rc.apply(state, rs.string("name").asInstanceOf[V])
              }
            }

            return state
          }

          def apply(closeMap: Map[String, Any]): Unit = {
            rc.apply(closeMap)
            conn.close()
          }

        }
      }
    }
  }

}

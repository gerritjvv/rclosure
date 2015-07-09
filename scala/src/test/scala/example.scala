package example

import rclosure._
import scalikejdbc._
import scala.collection.immutable.Map

object Example {

  Class.forName ("org.h2.Driver")
  ConnectionPool.singleton ("jdbc:h2:mem:hello", "user", "pass")


  def readFromDb[RET, V](rcg:RClosureGen[Map[String, Any], RET, V]): RClosureGen[Map[String, Any], RET, V] = {

    new RClosureGen[Map[String, Any], RET, V]{
      def apply(env: Map[String, Any]) = {
        val conn = ConnectionPool.borrow()

        val rc = rcg.apply(env)
        val init:RET = rc.apply()

        new RClosure [RET, V]{

          def apply() = { init }

          def apply(state: RET, v: V): RET = {
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

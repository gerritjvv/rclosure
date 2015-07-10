
import org.scalatest._
import scalikejdbc._
import example._
import rclosure._

import scala.collection.immutable.Map
import scala.collection.immutable.HashMap

class ExampleSpec extends FlatSpec with Matchers {

  Class.forName ("org.h2.Driver")
  ConnectionPool.singleton ("jdbc:h2:mem:hello", "user", "pass")

  def setupDb() = {

    DB localTx { implicit session =>
      sql"create table test (id bigint not null primary key, name varchar(100))".execute.apply()
      sql"insert into test values (1, 'ABC')".update.apply()
      sql"insert into test values (2, '123')  ".update.apply()
      sql"insert into test values (3, '456')".update.apply()
    }
  }

  "Resource closures" should "Compose" in {

    setupDb()
    val records:Int = RC.runOnce(HashMap("file" -> "/tmp/test-file.txt"),
                                 RC.rcompose(Example.writeToFile, Example.readFromDB))

    records should be (3)
  }

}

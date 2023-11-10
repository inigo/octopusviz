package net.surguy.octopusviz

import com.zaxxer.hikari.HikariDataSource
import net.surguy.octopusviz.storage.DatabaseAccess
import net.surguy.octopusviz.utils.Logging
import org.specs2.mutable.SpecLike
import org.specs2.specification.BeforeSpec
import org.specs2.specification.core.Fragments
import play.api.Configuration
import play.api.db.DBApi
import play.api.db.evolutions.OfflineEvolutions
import play.api.inject.guice.GuiceApplicationBuilder

import java.io.File
import java.util.concurrent.Executors
import javax.sql.DataSource
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}

trait UsesDatabase extends SpecLike with BeforeSpec with UsesConfig  {

  protected val dataSource: DataSource = HikariDataSource(dbConfig)
  protected val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  protected val db: DatabaseAccess = new DatabaseAccess(dataSource, ec)

  override def beforeSpec: Fragments = {
    val s = step({
      Await.result(ApplyEvolutions.from(Configuration(config)), 5.seconds) must not(throwAn[Exception])
    })
    Fragments(s)
  }

}


object ApplyEvolutions extends App with Logging {
  def from(config: play.api.Configuration) = {
    val app = new GuiceApplicationBuilder().configure(config).build()
    val dbApi = app.injector.instanceOf[DBApi]
    dbApi.databases().foreach { db =>
      log.info("Applying evolutions")
      OfflineEvolutions.applyScript(new File("."), this.getClass.getClassLoader, dbApi, db.name)
    }
    app.stop()
  }
}
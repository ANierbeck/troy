/*
 * Copyright 2016 Tamer AbdulRadi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package troy.driver

import java.util.UUID

import com.datastax.driver.core._

import scala.concurrent.{ Future, Await }
import scala.concurrent.duration.Duration

/*
 * Playground to test the helpers without macros
 */
object Usage extends App {
  val cluster = Cluster.builder().addContactPoint("127.0.0.1").build()
  implicit val session: Session = cluster.connect()
  case class Post(id: UUID, author_name: String, title: String)
  import scala.concurrent.ExecutionContext.Implicits.global

  val getByTitle = {
    import _root_.troy.driver.DriverHelpers._
    import _root_.troy.driver.Types

    val prepared =
      session.prepare("SELECT post_id, author_name, post_title FROM test.posts WHERE post_title = ?;")

    def parser(row: Row): Post =
      Post(
        column[UUID](0)(row).as[Types.Uuid],
        column[String](1)(row).as[Types.Text],
        column[String](2)(row).as[Types.Text]
      )

    (title: String) =>
      bind(prepared, param(title).as[Types.Text])
        .async
        .all
        .as(parser)
  }

  println(Await.result(getByTitle("Title"), Duration(1, "second")))
  session.close()
  cluster.close()
}
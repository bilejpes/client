import scalaj.http.{Http, HttpRequest, MultiPart}
import java.io._
import java.util


import scala.io.Source

/*
val s = "https://scala-budget.herokuapp.com"
type OptionMap = Map[Symbol, Any]
val m = Map('host -> "hostAddr", 'put -> "delFile")

mm(m)

def mm(opt: OptionMap): Unit = {
  println(opt)
  opt.toList match {
    case List(('del, _), _*) => println("del")
    case List(('put, _), _*) => println("put")
    case List(('get, _), _*) => println("get")
    case _ => mm(opt.tail)
  }
}*/

val request: HttpRequest =
  Http("qq")

val res = request.asString

request.


/*
val request: HttpRequest =
  Http("http://localhost:9000/api/records/hello2.txt")

val responseOne = request.asBytes.body

val res = request.asBytes.code

new String(responseOne)

val f = new File("C:\\Users\\H&M\\Documents\\test\\cv.pdf")
val bos = new BufferedOutputStream(new FileOutputStream(f))
Stream.continually(bos.write(arrB))
bos.close()
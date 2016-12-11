import java.io.{BufferedOutputStream, File, FileNotFoundException, FileOutputStream}
import java.net.UnknownHostException
import java.nio.file.{Files, Paths}

import scalaj.http.{Http, HttpOptions, HttpRequest}


object Client {
  val usage = """
    Usage: client (--put filepath | --del key | --get file [--dest destinationFolder]) hostname
  """

  type OptionMap = Map[Symbol, Any]


  def main(args: Array[String]) {

    /**
      * Recursively parse args
      * @param map parsed args
      * @param list remaining args (tail)
      * @return Map[Symbol, Any]
      */
    def parseArgs(map: OptionMap, list: List[String]) : OptionMap = {

      def isSwitch(s: String) = s(0) == '-'

      try {
        list match {
          case Nil => map
          case "--put" :: value :: tail if !map.contains('del) && !map.contains('get) =>
            parseArgs(map ++ Map('put -> value), tail)
          case "--del" :: value :: tail if !map.contains('put) && !map.contains('get) =>
            parseArgs(map ++ Map('del -> value), tail)
          case "--get" :: value :: tail if !map.contains('put) && !map.contains('del) =>
            parseArgs(map ++ Map('get -> value), tail)
          case "--dest" :: value :: tail if !map.contains('put) && !map.contains('del) =>
            parseArgs(map ++ Map('dest -> value), tail)
          case string :: opt2 :: _ if isSwitch(opt2) =>
            parseArgs(map ++ Map('host -> removeSlashOrBackSlash(string)), list.tail)
          case string :: Nil =>
            parseArgs(map ++ Map('host -> removeSlashOrBackSlash(string)), list.tail)
          case _ => printHelp
            sys.exit(1)
        }
      } catch {
        case _ : NumberFormatException => printHelp
      }
    }

    /**
      * prints help and exit
      */
    def printHelp = {
      println(usage)
      sys.exit(1)
    }


    if (args.length == 0) printHelp

    val options = parseArgs(Map(), args.toList)

    if(!options.contains('put) && !options.contains('del) && !options.contains('get)){
      printHelp
    }

    try {
      connect(options)
    } catch {
      case _ : java.lang.RuntimeException =>
        println(s"There was an error with connecting to ${options('host).toString}")
        sys.exit(1)
      case _ : FileNotFoundException =>
        println(s"File doesn't exist.")
        sys.exit(1)
      case _ : UnknownHostException =>
        println(s"Unknown host ${options('host).toString}")
        sys.exit(1)
      case x : Exception if x.toString.contains("NOAUTH Authentication required.") =>
        println("Authentication required.")
        sys.exit(1)
    }

  }

  /**
    * Removes slash or backslash from end of string
    * @param source string
    * @return if there was any change returns new string without slash,
    *         else returns itself
    */
  def removeSlashOrBackSlash(source: String) = {
    source(source.length-1) match {
      case '/' => source.substring(0, source.length-1)
      case '\\' => source.substring(0, source.length-1)
      case _ => source
    }
  }

  /**
    * Determinates which request to send according to arguments
    * @param options arguments
    */
  @throws(classOf[Exception])
  @throws(classOf[FileNotFoundException])
  @throws(classOf[UnknownHostException])
  def connect(options: OptionMap) = {
    if(options.contains('put))
      requestPut(options)
    else if(options.contains('del))
      requestDel(options)
    else
      requestGet(options)
  }

  /**
    * Sends a PUT request with body which contains file content as Array[Byte]
    * @param options arguments
    */
  @throws(classOf[FileNotFoundException])
  @throws(classOf[UnknownHostException])
  def requestPut(options: OptionMap) = {
    val file = options('put).toString
    val fileContent = Files.readAllBytes(Paths.get(options('put).toString))
    val fileName = file.substring(file.lastIndexOf("\\")+1)

    val request: HttpRequest =
      Http(options('host).toString + "/api/records")
        .headers(
          ("Content-Type", "application/octet-stream"),
          ("name", fileName)
        ).postData(fileContent).method("PUT")

    request.asString.code match {
      case 201 => println(s"201 - New file (${options('put).toString}) created.")
      case 204 => println(s"204 - Content of file (${options('put).toString}) changed.")
      case x => println(s"$x - No such file.")
        sys.exit(1)
    }
  }

  /**
    * Sends a DELETE request for deleting record from database
    * @param options arguments
    */
  @throws(classOf[UnknownHostException])
  def requestDel(options: OptionMap) = {
    val request: HttpRequest =
      Http(options('host).toString + "/api/records/" + options('del).toString).method("DELETE")
    request.asString.code match {
      case 200 => println(s"200 - File (${options('del).toString}) successfully deleted.")
      case x => println(s"$x - No such file.")
        sys.exit(1)
    }
  }

  /**
    * Sends a GET request for downloading file from database
    * @param options arguments
    */
  @throws(classOf[UnknownHostException])
  def requestGet(options: OptionMap) = {
    val request: HttpRequest =
      Http(options('host).toString + "/api/records/" + options('get).toString)
        .options(HttpOptions.readTimeout(10000), HttpOptions.connTimeout(10000))
    val response = request.asBytes
    response.code match {
      case 200 => {
        val f = options.get('dest) match {
          case Some(path) => new File(removeSlashOrBackSlash(path.toString)+"\\"+options('get).toString)
          case None => new File(options('get).toString)
        }
        val bos = new BufferedOutputStream(new FileOutputStream(f))
        Stream.continually(bos.write(response.body))
        bos.close()
        println(s"200 - File (${options('get).toString}) successfully downloaded.")
      }
      case x => println(s"$x - No such file.")
        sys.exit(1)
    }
  }
}

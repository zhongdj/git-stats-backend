package net.imadz.git.stats

trait MD5 {

  //way 2 md5.scala
  import java.security.MessageDigest

  val digest = MessageDigest.getInstance("MD5")

  //MD5 of text with updates
  def hash(xs: List[String]): String = {
    xs.foreach(x => digest.update(x.getBytes()))
    digest.digest().map(0xFF & _).map("%02x".format(_)).mkString
  }

}

package io.mediachain.translation

object DirectoryWalker {

  import java.io.File

  def walkTree(file: File): Vector[File] = {
    val children = new Iterable[File] {
      def iterator = if (file.isDirectory) file.listFiles.iterator else Iterator.empty
    }
    val files = Vector(file) ++: children.flatMap(walkTree)
    files.toVector
  }

  def findWithExtension(dir: File, ext: String): Vector[File] = {
    val lowerCaseExt = ext.toLowerCase
    walkTree(dir).filter(_.getName.toLowerCase.endsWith(lowerCaseExt))
  }
}
package net.imadz.git.stats.models

import net.imadz.git.stats.models.InvertedFileIndex._

sealed trait Node[T] {

  def parent(): Option[Node[T]]

  def children(): List[Node[T]]

  def add(aChild: Node[T]): Node[T]

  def minus(aChild: Node[T]): Node[T]

  def label(): T

  def upsert(newChild: Node[T]): Node[T] = minus(newChild).add(newChild)

  def child(aLabel: T): Option[Node[T]] = children().find(_.label == aLabel)

}

case class Root[T](override val label: T, override val children: List[Node[T]]) extends Node[T] {
  override def parent(): Option[Node[T]] = None

  override def add(aChild: Node[T]): Node[T] = child(aChild.label()).map(_ => this).getOrElse(copy(children = aChild :: this.children))

  override def minus(aChild: Node[T]): Node[T] = copy(children = children.filterNot(_.label() == aChild.label()))

  override def toString = s"Root(label: $label, children: $children)"

}

case class Leaf[T](override val label: T, parentNode: Node[T]) extends Node[T] {
  override def parent(): Option[Node[T]] = Some(parentNode)

  override def children(): List[Node[T]] = Nil

  override def add(aChild: Node[T]): Node[T] = this

  override def minus(aChild: Node[T]): Node[T] = this

  override def toString = s"Leaf(label: $label, children: $children)"
}

case class Middle[T](override val label: T, parentNode: Node[T], override val children: List[Node[T]]) extends Node[T] {
  override def parent(): Option[Node[T]] = Some(parentNode)

  override def add(aChild: Node[T]): Node[T] = {
    val option = child(aChild.label()).map(_ => this)
    val value = copy(children = aChild :: this.children)
    option.getOrElse(value)
  }

  override def minus(aChild: Node[T]): Node[T] = copy(children = children.filterNot(_.label() == aChild.label()))

  override def toString = s"Middle(label: $label, children: $children)"
}

object FileTree extends App {

  type File = java.io.File
  type Directory = String
  type FileTree = Root[File]
  type DirectoryNode = Middle[File]
  type FileNode = Leaf[File]

  def unit(name: File): FileTree = Root(name, Nil)

  private def segments(tree: Node[File], filePath: File) = {
    filePath.getAbsolutePath.diff(tree.label.getAbsolutePath)
      .split("/").filter(_.nonEmpty).toList
      .scanLeft(tree.label()) { case (parent, f) => new File(parent, f) }
      .tail
  }

  def addFile(parent: Node[File], filePath: File): (Node[File], Option[Node[File]]) = segments(parent, filePath) match {
    case Nil => (parent, None)
    case file :: Nil =>
      val leaf = Leaf(file, parent)
      (parent.add(leaf), Some(leaf))
    case file :: others =>
      val newChild = parent.child(file).map(theChild =>
        addFile(theChild, filePath)
      ).getOrElse(addFile(Middle(file, parent, Nil), filePath))
      (newChild._2.map(parent.upsert).getOrElse(parent), newChild._2)
  }

  def removeFile(filePath: File): FileTree = ???

  def mergeTree(one: FileTree, other: FileTree): FileTree = ???

}

case class InvertedFileIndex(projectRootPath: String) {

  import FileTree._

  var fileTree: Node[File] = Root(new java.io.File(projectRootPath), Nil)
  var fileIndex: Map[String, List[Node[File]]] = Map.empty.withDefaultValue(Nil)

  def rootedWith(file: Path) = file.startsWith(projectRootPath)

  def +(filePath: Path): InvertedFileIndex = {
    if (rootedWith(filePath)) {
      val file = new File(filePath)
      addFile(fileTree, file) match {
        case (fileTree: Node[File], theChild: Option[Node[File]]) =>
          theChild.foreach { it =>
            fileIndex += file.getName -> (it :: fileIndex(file.getName))
          }
      }
    }
    this
  }

  def singleIncompleteFile(abbrFilePath: String): Boolean = {
    abbrFilePath.indexOf(java.io.File.separator) == -1 && abbrFilePath.startsWith(".")
  }

  def suffix(abbrFilePath: String): String = abbrFilePath.replaceAll("""^.+""", "")

  def fullScan(abbrFilePath: String): Option[File] = fileIndex.find(_._1 endsWith suffix(abbrFilePath)).map(_._2.head.label())

  def search(abbrFilePath: String): Option[File] =
    if (singleIncompleteFile(abbrFilePath)) fullScan(abbrFilePath)
    else indexedSearch(abbrFilePath)

  val pattern = """\.*(.*)""".r

  private def indexedSearch(abbrFilePath: String) = {

    abbrFilePath.split(java.io.File.separator).filter(_.nonEmpty).toList.reverse match {
      case Nil => None
      case fileName :: Nil =>
        for {
          pattern(name) <- pattern.findFirstMatchIn(fileName)
          leaf <- fileIndex(name).headOption
        } yield leaf.label()
      case fileName :: dirs => fileIndex(fileName) match {
        case Nil => None
        case xs  => matchDirectories(dirs, xs)
      }
    }

  }

  def matchDirectories(dirs: List[Directory], xs: List[Node[File]]): Option[File] = xs match {
    case Leaf(it, parent) :: Nil =>
      matchDirectory(dirs, parent).map(_ => it)
    case Leaf(it, parent) :: xs =>
      matchDirectory(dirs, parent).map(_ => it).orElse(matchDirectories(dirs, xs))
  }

  def abbreviated: String => Boolean = _.startsWith(".")

  def abbreviationMatched(full: Node[File]): Directory => Option[File] = abbrDir => {
    val suffix = abbrDir.replaceAll("""\.+""", "")
    if (full.label().getName.endsWith(suffix)) Some(full.label())
    else None
  }

  def matchDirectory(dirs: List[Directory], matching: Node[File]): Option[File] =
    dirs match {
      case dir :: Nil =>
        if (abbreviated(dir))
          abbreviationMatched(matching)(dir)
        else if (dir == matching.label().getName) Some(matching.label) else None
      case dir :: others => for {
        next <- matching.parent()
        d <- if (dir == matching.label().getName) matchDirectory(others, next) else None
      } yield d
    }

  def find(file: PathSuffix): Option[Path] = ???

  protected def fileName(file: Path): FileName = ???

  protected def parentList(file: Path): List[ParentList] = ???
}

object InvertedFileIndex {
  type Path = String
  type PathSuffix = String
  type FileName = String
  type DirectoryName = String
  type ParentList = List[DirectoryName]

  def apply(files: List[Path]): InvertedFileIndex = ???
}

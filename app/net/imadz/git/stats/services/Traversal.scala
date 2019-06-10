package net.imadz.git.stats.services

trait Traversal {
  def sequence[E, A](es: List[Either[E, A]]): Either[E, List[A]] =
    traverse(es)(x => x)

  private def traverse[E, A, B](es: List[A])(f: A => Either[E, B]): Either[E, List[B]] =
    es match {
      case Nil    => Right(Nil)
      case h :: t => map2(f(h), traverse(t)(f))(_ :: _)
    }

  private def map2[E, EE >: E, A, B, C](a: Either[E, A], b: Either[EE, B])(f: (A, B) => C): Either[EE, C] = for { a1 <- a; b1 <- b } yield f(a1, b1)
}

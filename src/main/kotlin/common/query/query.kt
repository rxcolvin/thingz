package common.query

import expression.Expr

interface QueryDef<T>

object QueryAll : QueryDef<Nothing>


class FilterQueryDef<E> (
     val filter: (E) -> Boolean
) : QueryDef<E>

class ExprQueryDef<E> (
    val expr: Expr
) : QueryDef<E>
package models.repository

case class ListRepositoryRec(rec: RepositoryRec, countOfRecsInSequence: Int, actions: List[ActionRec], modifiedBy: Set[String])

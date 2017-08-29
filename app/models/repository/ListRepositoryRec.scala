package models.repository

/**
  * Created by Sergey Tarkhanov on 5/19/2015.
  */

case class ListRepositoryRec(rec: RepositoryRec, countOfRecsInSequence: Int, actions: List[ActionRec], modifiedBy: Set[String])

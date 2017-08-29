package models.repository

/**
  * Created by Sergey Tarkhanov on 5/19/2015.
  */

case class DetailsRepositoryRec(session: Option[SessionRec], rec: RepositoryRec, history: Seq[DetailsRepositoryItem], historyCount: Int)
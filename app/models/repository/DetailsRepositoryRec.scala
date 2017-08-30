package models.repository

case class DetailsRepositoryRec(session: Option[SessionRec], rec: RepositoryRec, history: Seq[DetailsRepositoryItem], historyCount: Int)
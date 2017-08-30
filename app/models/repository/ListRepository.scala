package models.repository

case class ListRepository(session: Option[SessionRec], records: Seq[ListRepositoryRec], recordCount: Int)

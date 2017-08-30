package models.repository

case class RepositoryRec(id: Long, seq: String, kind: String, name: String, active: Boolean = false, parentId: Option[Long] = None)

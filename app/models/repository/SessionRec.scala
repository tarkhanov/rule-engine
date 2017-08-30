package models.repository

import java.sql.Timestamp

case class SessionRec(id: Long, user: String, creationDate: Timestamp, commitDate: Option[Timestamp] = None)

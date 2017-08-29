package models.repository

import java.sql.Timestamp

/**
 * Created by Sergey Tarkhanov on 5/26/2015.
 */

case class SessionRec(id: Long, user: String, creationDate: Timestamp, commitDate: Option[Timestamp] = None)

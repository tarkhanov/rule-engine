package models.repository

/**
 * Created by Sergey Tarkhanov on 5/19/2015.
 */

case class RepositoryRec(id: Long, seq: String, kind: String, name: String, active: Boolean = false, parentId: Option[Long] = None)

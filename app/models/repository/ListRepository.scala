package models.repository

/**
  * Created by Sergey Tarkhanov on 5/19/2015.
  */

case class ListRepository(session: Option[SessionRec], records: Seq[ListRepositoryRec], recordCount: Int)

package models.repository

/**
  * Created by Sergey Tarkhanov on 5/19/2015.
  */

case class DetailsRepositoryItem(rec: RepositoryRec, actions: List[(ActionRec, SessionRec)])

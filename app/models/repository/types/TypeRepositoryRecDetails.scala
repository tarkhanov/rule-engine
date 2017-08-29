package models.repository.types

import models.repository.ActionRec

/**
 * Created by Sergey Tarkhanov on 6/3/2015.
 */

case class TypeRepositoryRecDetails(record: TypeRepositoryRec, actions: List[ActionRec])

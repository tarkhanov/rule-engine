package models.repository.types

import models.repository.ActionRec

case class TypeRepositoryRecDetails(record: TypeRepositoryRec, actions: List[ActionRec])

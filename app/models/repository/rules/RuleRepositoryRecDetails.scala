package models.repository.rules

import models.repository.ActionRec

/**
 * Created by Sergey Tarkhanov on 6/3/2015.
 */
case class RuleRepositoryRecDetails(record: RuleRepositoryRec, actions: List[ActionRec])

package models.repository.rules

import models.repository.ActionRec

case class RuleRepositoryRecDetails(record: RuleRepositoryRec, actions: List[ActionRec])

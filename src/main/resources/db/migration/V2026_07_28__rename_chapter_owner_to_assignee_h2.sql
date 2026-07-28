-- H2 equivalent: rename OwnerId -> AssigneeId on Chapter.
ALTER TABLE [Chapter] ALTER COLUMN [OwnerId] RENAME TO [AssigneeId];
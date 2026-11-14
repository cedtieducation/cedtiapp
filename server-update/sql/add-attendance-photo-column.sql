-- Take a database backup first. Run this once in the CEDTI attendance database.
ALTER TABLE attendance
  ADD COLUMN photo_path VARCHAR(255) NULL AFTER longitude;

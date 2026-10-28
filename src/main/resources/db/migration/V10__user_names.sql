-- Editable name parts for user profiles.
ALTER TABLE users ADD COLUMN IF NOT EXISTS first_name VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_name  VARCHAR(50);

UPDATE users
SET first_name = split_part(trim(full_name), ' ', 1),
    last_name  = NULLIF(
        trim(substr(trim(full_name), length(split_part(trim(full_name), ' ', 1)) + 1)),
        ''
    )
WHERE full_name IS NOT NULL
  AND trim(full_name) <> ''
  AND first_name IS NULL;

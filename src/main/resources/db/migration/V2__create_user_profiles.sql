CREATE TABLE IF NOT EXISTS user_profiles (
    username VARCHAR(60) PRIMARY KEY,
    display_name VARCHAR(60),
    profile_picture BLOB,
    image_content_type VARCHAR(100)
);
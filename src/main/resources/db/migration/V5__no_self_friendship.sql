-- Nobody can be their own friend or invite themselves. The application
-- refuses it (UserServiceImpl), but before that check a forged request could
-- create such rows. Remove any that exist and let the database refuse new ones.
DELETE FROM friendships WHERE user_login = friend_login;
DELETE FROM friend_invites WHERE user_login = inviter_login;

ALTER TABLE friendships
    ADD CONSTRAINT chk_friendships_not_self CHECK (user_login <> friend_login);

ALTER TABLE friend_invites
    ADD CONSTRAINT chk_friend_invites_not_self CHECK (user_login <> inviter_login);

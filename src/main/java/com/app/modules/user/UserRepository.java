package com.app.modules.user;

import com.app.core.BaseRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserRepository extends BaseRepository<UserModel> {
    public UserRepository() {
    }

    public UserModel findByEmail(String email) {
        return find("email", email).firstResult();
    }
}

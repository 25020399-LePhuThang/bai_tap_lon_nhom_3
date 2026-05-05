package com.auction.client.UIcontroller;

import com.auction.shared.model.user.User;

import java.util.List;

public class UserManager {
    private UserManager UserManagerInstance;

    private UserManager(){}

    public UserManager getInstance(){
        if(UserManagerInstance==null){
            UserManagerInstance=new UserManager();
        }
        return UserManagerInstance;
    }

    public void SignIn(){
         List<User> UserList;
    }

    public void Register(){}
}

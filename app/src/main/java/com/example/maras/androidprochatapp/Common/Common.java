package com.example.maras.androidprochatapp.Common;

import com.example.maras.androidprochatapp.Holder.QBUsersHolder;
import com.quickblox.users.model.QBUser;


import java.util.List;

/**
 * Created by MARAS on 27.06.2017.
 */

public class Common {

    public static final String DIALOG_EXTRA = "Dialogs";

    //Tworzenie nazwy czatu dla uzytkownikow z listy

    public static String createChatDialogsName(List<Integer> qbUsers)
    {
        List<QBUser> qbUsers1 = QBUsersHolder.getInstance().getUsersByIds(qbUsers);
        StringBuilder name = new StringBuilder();
        //Ustawiamy nazwe czatu jaka polaczone nazwy uzytkownikow bioracych w nim udzial
        for(QBUser user:qbUsers1)
            name.append(user.getFullName());
        if(name.length() > 30)
            name = name.replace(30,name.length()-1,"...");
        return name.toString();
    }
}
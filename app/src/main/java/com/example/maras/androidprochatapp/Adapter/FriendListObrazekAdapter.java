package com.example.maras.androidprochatapp.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.example.maras.androidprochatapp.Holder.QBUnreadMessageHolder;
import com.example.maras.androidprochatapp.Holder.QBUsersHolder;
import com.example.maras.androidprochatapp.R;
import com.quickblox.content.QBContent;
import com.quickblox.content.model.QBFile;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by MARAS on 27.06.2017.
 */

public class FriendListObrazekAdapter extends BaseAdapter{

    private Context context;
    private ArrayList<QBUser> qbUserArrayList;

    public FriendListObrazekAdapter(Context context, ArrayList<QBUser> qbUserArrayList) {
        this.context = context;
        this.qbUserArrayList = qbUserArrayList;
    }

    @Override
    public int getCount() {
        return qbUserArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return qbUserArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if(convertView == null){
            boolean WTF = false;
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_chat_users_avatars, null);

            TextView txtTitle, txtMessage;
            final ImageView imageView;

            txtMessage = (TextView) view.findViewById(R.id.list_chat_dialog_message_obrazek);
            txtTitle = (TextView) view.findViewById(R.id.list_chat_dialog_title_obrazek);

            imageView = (ImageView)view.findViewById(R.id.image_obrazek);


            txtMessage.setText(qbUserArrayList.get(position).getFullName());
            txtTitle.setText(qbUserArrayList.get(position).getLogin());


            ColorGenerator generator = ColorGenerator.DEFAULT;
            int randomColor = generator.getRandomColor();


            //QBUsersHolder.getInstance().putUser(qbUser);

            /*if(QBUsers.getUser(qbUserArrayList.get(position).getFileId())==null)
            {
                TextDrawable.IBuilder builder = TextDrawable.builder().beginConfig()
                        .withBorder(4)
                        .endConfig()
                        .round();

                //Pobierz pierwsza litere z CHat Dialog Title aby utworzyc Chat Dialog Image
                TextDrawable drawable = builder.build(txtTitle.getText().toString().substring(0, 1).toUpperCase(), randomColor);
                if(imageView != null)
                imageView.setImageDrawable(drawable);
            }
            else

            {*/

                QBUsers.getUser(qbUserArrayList.get(position).getId())
                        .performAsync(new QBEntityCallback<QBUser>() {
                            @Override
                            public void onSuccess(QBUser qbUser, Bundle bundle) {
                                int profilePictureId = qbUserArrayList.get(position).getFileId();
                                QBContent.getFile(profilePictureId)
                                        .performAsync(new QBEntityCallback<QBFile>() {
                                            @Override
                                            public void onSuccess(QBFile qbFile, Bundle bundle) {
                                                String fileUrl = qbFile.getPublicUrl();
                                                if(fileUrl != null) {
                                                    if(imageView != null) {
                                                        Picasso.with(context)
                                                                .load(fileUrl)
                                                                .into(imageView);
                                                    }
                                                }

                                            }

                                            @Override
                                            public void onError(QBResponseException e) {

                                            }
                                        });
                            }

                            @Override
                            public void onError(QBResponseException e) {

                            }
                        });


            //}

            //ilosc nowych wiadomosci




        }
        return view;
    }
}

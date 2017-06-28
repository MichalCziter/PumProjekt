package com.example.maras.androidprochatapp;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.example.maras.androidprochatapp.Adapter.ChatMessageAdapter;
import com.example.maras.androidprochatapp.Common.Common;
import com.example.maras.androidprochatapp.Holder.QBChatMessagesHolder;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBIncomingMessagesManager;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.exception.QBChatException;
import com.quickblox.chat.listeners.QBChatDialogMessageListener;
import com.quickblox.chat.listeners.QBChatDialogParticipantListener;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.chat.model.QBPresence;
import com.quickblox.chat.request.QBDialogRequestBuilder;
import com.quickblox.chat.request.QBMessageGetBuilder;
import com.quickblox.chat.request.QBMessageUpdateBuilder;
import com.quickblox.content.QBContent;
import com.quickblox.content.model.QBFile;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.QBRequestUpdateBuilder;
import com.squareup.picasso.Picasso;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.DiscussionHistory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

public class ChatMessageActivity extends AppCompatActivity implements QBChatDialogMessageListener{

    QBChatDialog qbChatDialog;
    ListView lstChatMessages;
    ImageButton sumbitButton;
    EditText edtContent, newName;

    ChatMessageAdapter adapter;

    //Proba ogarniecia userow ktorzy sa online
    ImageView img_online_count, dialog_avatar;
    TextView txt_online_count;




    //Probujemy dodac opcje z Update/Usuwaniem wiadomosci
    int contextMenuIndexClicked = -1;
    boolean isEditMode = false;
    QBChatMessage editMessage;

    //Update dialog
    Toolbar toolbar;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Trzeba sprawdzic czy dialog jest grupowy
        if(qbChatDialog.getType() == QBDialogType.GROUP || qbChatDialog.getType() == QBDialogType.PUBLIC_GROUP)
            getMenuInflater().inflate(R.menu.chat_message_group_menu,menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId())
        {
            case R.id.chat_group_edit_name:
                editNameGroup();
                break;
            case R.id.chat_group_add_user:
                addUser();
                break;
            case R.id.chat_group_remove_user:
                removeUser();
                break;
        }

        return true;
    }

    private void removeUser() {

        Intent intent = new Intent(this,ListUsersActivity.class);
        intent.putExtra(Common.UPDATE_DIALOG_EXTRA,qbChatDialog);
        intent.putExtra(Common.UPDATE_MODE,Common.UPDATE_REMOVE_MODE);
        startActivity(intent);

    }

    private void addUser() {
        //Wyswietlamy wszystkich userow ktorzy nie sa w grupie jako liste
        //odpalamy nowe ListUserActivity i zmieniamy jego źródło
        Intent intent = new Intent(this,ListUsersActivity.class);
        intent.putExtra(Common.UPDATE_DIALOG_EXTRA,qbChatDialog);
        intent.putExtra(Common.UPDATE_MODE,Common.UPDATE_ADD_MODE);
        startActivity(intent);
    }

    private void editNameGroup() {

        //tworzymy wyskakujace okienko gdzie mozna wpisac nowa nazwe pokoju
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_edit_group_layout,null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(view);
        final EditText newName = (EditText)view.findViewById(R.id.edt_new_group_name);

        //Toast.makeText(this, ""+newName.getText().toString(), Toast.LENGTH_SHORT).show();

        //Dialog message
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Uzytkownik klika OK - bierzemy text z edittext i ustawiamy jako nazwe czatu
                        qbChatDialog.setName(newName.getText().toString());

                        QBDialogRequestBuilder requestBuilder = new QBDialogRequestBuilder();
                        QBRestChatService.updateGroupChatDialog(qbChatDialog,requestBuilder)
                                .performAsync(new QBEntityCallback<QBChatDialog>() {
                                    @Override
                                    public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                        Toast.makeText(ChatMessageActivity.this, "Zmieniono nazwe grupy", Toast.LENGTH_SHORT).show();
                                        toolbar.setTitle(qbChatDialog.getName());


                                    }

                                    @Override
                                    public void onError(QBResponseException e) {
                                        Toast.makeText(getBaseContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        //Tworzymy Alert Dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();


    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        //Trzeba przypisac ineks kliknietej rzeczy z menu kontekstowego go zmiennej globalnej
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        contextMenuIndexClicked = info.position;


        switch (item.getItemId())
        {
            case R.id.chat_message_update_message:
                updateMessage();
                break;
            case R.id.chat_message_delete_message:
                deleteMessage();
                break;
        }

        return true;
    }

    private void deleteMessage() {

        final ProgressDialog deleteDialog = new ProgressDialog(ChatMessageActivity.this);
        deleteDialog.setMessage("Pleade wait...");
        deleteDialog.show();

        editMessage = QBChatMessagesHolder.getInstance().getChatMessagesByDialogId(qbChatDialog.getDialogId())
                .get(contextMenuIndexClicked);

        QBRestChatService.deleteMessage(editMessage.getId(),false).performAsync(new QBEntityCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid, Bundle bundle) {
                retrieveMessage();
                deleteDialog.dismiss();
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });
    }

    private void updateMessage() {
        //korzystajac z indeksu z menu context mozemy wyciagnac wiadomosc z cache
        editMessage = QBChatMessagesHolder.getInstance().getChatMessagesByDialogId(qbChatDialog.getDialogId())
                .get(contextMenuIndexClicked);
        edtContent.setText(editMessage.getBody());
        //trzeba ustawic true ze wiadomosc jest w edit mode
        isEditMode = true;

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.chat_message_context_menu,menu);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        qbChatDialog.removeMessageListrener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        qbChatDialog.removeMessageListrener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_message);

        initViews();
        
        initChatDialogs();

        //Zaladuj wszystkie wiadomosci z rozmowy
        
        retrieveMessage();

        sumbitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!edtContent.getText().toString().isEmpty()) {
                    if (!isEditMode) {

                        QBChatMessage chatMessage = new QBChatMessage();
                        chatMessage.setBody(edtContent.getText().toString());
                        chatMessage.setSenderId(QBChatService.getInstance().getUser().getId());
                        chatMessage.setSaveToHistory(true);

                        try {
                            qbChatDialog.sendMessage(chatMessage);
                        } catch (SmackException.NotConnectedException e) {
                            e.printStackTrace();
                        }

                        //Zbugowany prywatny czat nie pokazuje wiadomosci
                        if (qbChatDialog.getType() == QBDialogType.PRIVATE) {

                            //Zapisujemy wiadomosc w pamieci cache i odswiezamy list view
                            QBChatMessagesHolder.getInstance().putMessage(qbChatDialog.getDialogId(), chatMessage);
                            ArrayList<QBChatMessage> messages = QBChatMessagesHolder.getInstance().getChatMessagesByDialogId(chatMessage.getDialogId());
                            adapter = new ChatMessageAdapter(getBaseContext(), messages);
                            lstChatMessages.setAdapter(adapter);
                            adapter.notifyDataSetChanged();

                        }


                        //Usun text z edit text
                        edtContent.setText("");
                        edtContent.setFocusable(true);

                    } else {

                        final ProgressDialog updateDialog = new ProgressDialog(ChatMessageActivity.this);
                        updateDialog.setMessage("Pleade wait...");
                        updateDialog.show();

                        //kodzik jezeli jest w edit mode
                        QBMessageUpdateBuilder messageUpdateBuilder = new QBMessageUpdateBuilder();
                        messageUpdateBuilder.updateText(edtContent.getText().toString()).markDelivered().markRead();
                        QBRestChatService.updateMessage(editMessage.getId(), qbChatDialog.getDialogId(), messageUpdateBuilder)
                                .performAsync(new QBEntityCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid, Bundle bundle) {
                                        //Odswierzamy dane z serwera
                                        retrieveMessage();
                                        isEditMode = false;
                                        updateDialog.dismiss();

                                        //czyscimy edittext
                                        edtContent.setText("");
                                        edtContent.setFocusable(true);

                                    }

                                    @Override
                                    public void onError(QBResponseException e) {
                                        Toast.makeText(getBaseContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });


                    }
                }
            }
        });

        

    }

    private void retrieveMessage() {

        QBMessageGetBuilder messageGetBuilder = new QBMessageGetBuilder();
        messageGetBuilder.setLimit(500); //wczyta max 50 mozna zmienic

        if(qbChatDialog != null)
        {
            QBRestChatService.getDialogMessages(qbChatDialog, messageGetBuilder).performAsync(new QBEntityCallback<ArrayList<QBChatMessage>>() {
                @Override
                public void onSuccess(ArrayList<QBChatMessage> qbChatMessages, Bundle bundle) {

                    //Zapisz liste wiadomosci do cache i odswiez list view
                    QBChatMessagesHolder.getInstance().putMessages(qbChatDialog.getDialogId(),qbChatMessages);

                    adapter = new ChatMessageAdapter(getBaseContext(),qbChatMessages);
                    lstChatMessages.setAdapter(adapter);
                    adapter.notifyDataSetChanged();

                }

                @Override
                public void onError(QBResponseException e) {

                }
            });
        }

    }

    private void initChatDialogs() {

        qbChatDialog = (QBChatDialog)getIntent().getSerializableExtra(Common.DIALOG_EXTRA);

        //Trzeba pobrac obrazek z servera i ustawic dla rozmowy
        if(qbChatDialog.getPhoto() == null)
        {
            QBContent.getFile(Integer.parseInt(qbChatDialog.getPhoto()))
                    .performAsync(new QBEntityCallback<QBFile>() {
                        @Override
                        public void onSuccess(QBFile qbFile, Bundle bundle) {
                            String fileURL = qbFile.getPublicUrl();
                            Picasso.with(getBaseContext())
                                    .load(fileURL)
                                    .resize(50,50)
                                    .centerCrop()
                                    .into(dialog_avatar);
                        }

                        @Override
                        public void onError(QBResponseException e) {
                            Log.e("ErrorPobieObrazka",""+e.getMessage());

                        }
                    });
        }


        qbChatDialog.initForChat(QBChatService.getInstance());

        //Tworzymy listener do przetwarzania przychodzacych wiadomosci

        QBIncomingMessagesManager incomingMessage = QBChatService.getInstance().getIncomingMessagesManager();
        incomingMessage.addDialogMessageListener(new QBChatDialogMessageListener() {
            @Override
            public void processMessage(String s, QBChatMessage qbChatMessage, Integer integer) {

            }

            @Override
            public void processError(String s, QBChatException e, QBChatMessage qbChatMessage, Integer integer) {

            }
        });

        //dodanie do grupy czatowej
        if(qbChatDialog.getType() == QBDialogType.PUBLIC_GROUP || qbChatDialog.getType() == QBDialogType.GROUP)
        {
            DiscussionHistory discussionHistory = new DiscussionHistory();
            discussionHistory.setMaxStanzas(0);

            qbChatDialog.join(discussionHistory, new QBEntityCallback() {
                @Override
                public void onSuccess(Object o, Bundle bundle) {

                }

                @Override
                public void onError(QBResponseException e) {
                    Log.e("ERROR", " "+ e.getMessage());
                }
            });


        }

        final QBChatDialogParticipantListener participantListener = new QBChatDialogParticipantListener() {
            @Override
            public void processPresence(String dialogId, QBPresence qbPresence) {
                if(dialogId == qbChatDialog.getDialogId())
                {
                    QBRestChatService.getChatDialogById(dialogId)
                            .performAsync(new QBEntityCallback<QBChatDialog>() {
                                @Override
                                public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                    //pobieramy userow online mam nadzieje bo nie ogarniam co sie dzieje w tutaj xD
                                    try {
                                        Collection<Integer> onlineList = qbChatDialog.getOnlineUsers();

                                        TextDrawable.IBuilder builder = TextDrawable.builder()
                                                .beginConfig()
                                                .withBorder(4)
                                                .endConfig()
                                                .round();
                                        TextDrawable online = builder.build("", Color.RED);

                                        img_online_count.setImageDrawable(online);

                                        txt_online_count.setText(String.format("%d/%d online",onlineList.size(),qbChatDialog.getOccupants().size()));



                                    } catch (XMPPException e) {
                                        e.printStackTrace();
                                    }


                                }

                                @Override
                                public void onError(QBResponseException e) {

                                }
                            });

                }
            }
        };

        qbChatDialog.addParticipantListener(participantListener);


        qbChatDialog.addMessageListener(this);

        //Ustawiamy tytul toolbara
        toolbar.setTitle(qbChatDialog.getName());
        setSupportActionBar(toolbar);






    }

    private void initViews() {

        lstChatMessages = (ListView)findViewById(R.id.list_of_messages);
        sumbitButton = (ImageButton)findViewById(R.id.send_button);
        edtContent = (EditText)findViewById(R.id.edt_content);

        img_online_count = (ImageView)findViewById(R.id.img_online_count);

        txt_online_count = (TextView)findViewById(R.id.txt_online_count);

        //Obrazek dla grupy czatowej
        dialog_avatar = (ImageView)findViewById(R.id.dialog_avatar);
        dialog_avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent selectImage = new Intent();
                selectImage.setType("image/*");
                selectImage.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(selectImage,"Wybierz Obrazek"),Common.SELECT_PICTURE);
            }
        });

        //menu kontekstowe edycji/uswania wiadomosci
        registerForContextMenu(lstChatMessages);

        //Dodajemy Toolbar
        toolbar = (Toolbar)findViewById(R.id.chat_message_toolbar);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK)
        {
            if(requestCode == Common.SELECT_PICTURE)
            {
                Uri selectedImageUri = data.getData();
                final ProgressDialog mDialog = new ProgressDialog(ChatMessageActivity.this);
                mDialog.setMessage("Prosze czekac");
                mDialog.setCancelable(false);
                mDialog.show();

                try{
                    //konwersja uri do pliku
                    InputStream in = getContentResolver().openInputStream(selectedImageUri);
                    final Bitmap bitmap = BitmapFactory.decodeStream(in);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,bos);
                    File file = new File(Environment.getExternalStorageDirectory()+"/image.png");
                    FileOutputStream fileOut = new FileOutputStream(file);
                    fileOut.write(bos.toByteArray());
                    fileOut.flush();
                    fileOut.close();

                    //trza sprawdzic rozmiar obrazka

                    int imageSizeKb = (int)file.length()/1024;
                    
                    if(imageSizeKb >= (1024*100))
                    {
                        Toast.makeText(this, "Error rozmiaru", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //Wrzucamy plik na server
                    QBContent.uploadFileTask(file,true,null)
                            .performAsync(new QBEntityCallback<QBFile>() {
                                @Override
                                public void onSuccess(QBFile qbFile, Bundle bundle) {
                                    //Jak sukces to ustawiamy fote jako fote rozmowy
                                    qbChatDialog.setPhoto(qbFile.getId().toString());

                                    QBRequestUpdateBuilder requestBuilder = new QBRequestUpdateBuilder();
                                    QBRestChatService.updateGroupChatDialog(qbChatDialog, requestBuilder)
                                            .performAsync(new QBEntityCallback<QBChatDialog>() {
                                                @Override
                                                public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                                    mDialog.dismiss();
                                                    dialog_avatar.setImageBitmap(bitmap);
                                                }

                                                @Override
                                                public void onError(QBResponseException e) {
                                                    Toast.makeText(ChatMessageActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }

                                @Override
                                public void onError(QBResponseException e) {

                                }
                            });



                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void processMessage(String s, QBChatMessage qbChatMessage, Integer integer) {
        //Zapisujemy wiadomosc w pamieci cache i odswiezamy list view
        QBChatMessagesHolder.getInstance().putMessage(qbChatMessage.getDialogId(),qbChatMessage);
        ArrayList<QBChatMessage> messages = QBChatMessagesHolder.getInstance().getChatMessagesByDialogId(qbChatMessage.getDialogId());
        adapter = new ChatMessageAdapter(getBaseContext(),messages);
        lstChatMessages.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void processError(String s, QBChatException e, QBChatMessage qbChatMessage, Integer integer) {
        Log.e("ERROR"," " + e.getMessage());
    }
}

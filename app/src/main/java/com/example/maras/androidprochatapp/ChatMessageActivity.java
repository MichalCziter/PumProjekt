package com.example.maras.androidprochatapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.example.maras.androidprochatapp.Adapter.ChatMessageAdapter;
import com.example.maras.androidprochatapp.Common.Common;
import com.example.maras.androidprochatapp.Holder.QBChatMessagesHolder;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBIncomingMessagesManager;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.exception.QBChatException;
import com.quickblox.chat.listeners.QBChatDialogMessageListener;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.request.QBMessageGetBuilder;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;

import org.jivesoftware.smack.SmackException;

import java.util.ArrayList;

public class ChatMessageActivity extends AppCompatActivity {

    QBChatDialog qbChatDialog;
    ListView lstChatMessages;
    ImageButton sumbitButton;
    EditText edtContent;

    ChatMessageAdapter adapter;



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
                QBChatMessage chatMessage = new QBChatMessage();
                chatMessage.setBody(edtContent.getText().toString());
                chatMessage.setSenderId(QBChatService.getInstance().getUser().getId());
                chatMessage.setSaveToHistory(true);

                try {
                    qbChatDialog.sendMessage(chatMessage);
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }

                //Zaladuj do cache i odswiez list view
                QBChatMessagesHolder.getInstance().putMessage(qbChatDialog.getDialogId(),chatMessage);
                ArrayList<QBChatMessage> messages = QBChatMessagesHolder.getInstance().getChatMessagesByDialogId(qbChatDialog.getDialogId());
                adapter = new ChatMessageAdapter(getBaseContext(),messages);
                lstChatMessages.setAdapter(adapter);
                adapter.notifyDataSetChanged();

                //Usun text z edit text
                edtContent.setText("");
                edtContent.setFocusable(true);
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

        qbChatDialog.addMessageListener(new QBChatDialogMessageListener() {
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
                Log.e("ERROR",e.getMessage());
            }
        });

    }

    private void initViews() {

        lstChatMessages = (ListView)findViewById(R.id.list_of_messages);
        sumbitButton = (ImageButton)findViewById(R.id.send_button);
        edtContent = (EditText)findViewById(R.id.edt_content);


    }
}

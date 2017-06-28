package com.example.maras.androidprochatapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.maras.androidprochatapp.Common.Common;
import com.quickblox.chat.QBChatService;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

public class UserProfile extends AppCompatActivity {


    EditText edtPassword, edtOldPassword, edtFullName, edtEmail, edtPhone;
    Button btnUpdate, btnCancel;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user_update_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
            case R.id.user_update_log_out:
                logOut();
                break;
            default:
                break;
        }

        return true;
    }

    private void logOut() {
        QBUsers.signOut().performAsync(new QBEntityCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid, Bundle bundle) {
                //jeżeli sukces to trzeba sie wylogowac tak jak kaza z tej bazy danych
                QBChatService.getInstance().logout(new QBEntityCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid, Bundle bundle) {
                        Toast.makeText(UserProfile.this, "Wylogowales sie!", Toast.LENGTH_SHORT).show();
                        //przechodzimy do mainactivity
                        Intent intent = new Intent(UserProfile.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //Usun wszystkie wczesniejsze activity
                        startActivity(intent);
                        finish();
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

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        //Dodajemy toolbar
        Toolbar toolbar = (Toolbar)findViewById(R.id.user_update_toolbar);
        toolbar.setTitle("Android Pro Chat");
        setSupportActionBar(toolbar);

        initViews();

        //Pobierzemy profil uzytkownika z bazy danych
        loadUserProfile();

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //jezeli cancel to po prostu zamykamy i elo
                finish();
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password = edtPassword.getText().toString();
                String oldPassword = edtOldPassword.getText().toString();
                String email = edtEmail.getText().toString();
                String phone = edtPhone.getText().toString();
                String fullName = edtFullName.getText().toString();

                //Odczytujemy Id klienta ktory jest zalogowany
                QBUser user = new QBUser();
                user.setId(QBChatService.getInstance().getUser().getId());
                //Jezeli jakis tekst ie jest null lub empty to sie bierzemy do roboty
                if(!Common.isNullOrEmptyString(oldPassword));
                    user.setOldPassword(oldPassword);
                if(!Common.isNullOrEmptyString(password));
                    user.setPassword(password);
                if(!Common.isNullOrEmptyString(fullName));
                    user.setFullName(fullName);
                if(!Common.isNullOrEmptyString(email));
                    user.setEmail(email);
                if(!Common.isNullOrEmptyString(phone));
                    user.setPhone(phone);

                final ProgressDialog mDialog = new ProgressDialog(UserProfile.this);


                mDialog.setMessage("Please wait...");
                mDialog.show();

                QBUsers.updateUser(user).performAsync(new QBEntityCallback<QBUser>() {
                    @Override
                    public void onSuccess(QBUser qbUser, Bundle bundle) {
                        Toast.makeText(UserProfile.this, "User"+qbUser.getLogin()+"updated", Toast.LENGTH_SHORT).show();
                        mDialog.dismiss();


                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Toast.makeText(UserProfile.this, "ERROR:"+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });



            }
        });






    }

    private void loadUserProfile() {

        QBUser currentUser = QBChatService.getInstance().getUser();
        String fullName = currentUser.getFullName();
        String email = currentUser.getEmail();
        String phone = currentUser.getPhone();

        edtEmail.setText(email);
        edtFullName.setText(fullName);
        edtPhone.setText(phone);


    }

    private void initViews() {
        btnCancel = (Button)findViewById(R.id.update_user_btn_cancel);
        btnUpdate = (Button)findViewById(R.id.update_user_btn_update);

        edtEmail = (EditText)findViewById(R.id.update_edt_email);
        edtPhone = (EditText)findViewById(R.id.update_edt_phone);
        edtFullName = (EditText)findViewById(R.id.update_edt_full_name);
        edtPassword = (EditText)findViewById(R.id.update_edt_password);
        edtOldPassword = (EditText)findViewById(R.id.update_edt_old_password);







    }
}

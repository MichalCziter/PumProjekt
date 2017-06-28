package com.example.maras.androidprochatapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.maras.androidprochatapp.Common.Common;
import com.example.maras.androidprochatapp.Holder.QBUsersHolder;
import com.quickblox.chat.QBChatService;
import com.quickblox.content.QBContent;
import com.quickblox.content.model.QBFile;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class UserProfile extends AppCompatActivity {


    EditText edtPassword, edtOldPassword, edtFullName, edtEmail, edtPhone;
    Button btnUpdate, btnCancel;

    ImageView user_avatar;



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

        user_avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Wybierz Obrazek"),Common.SELECT_PICTURE);
            }
        });

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK)
        {
            if(requestCode == Common.SELECT_PICTURE)
            {
                Uri selectedImageUri = data.getData();

                final ProgressDialog mDialog = new ProgressDialog(UserProfile.this);
                mDialog.setMessage("Prosze Czekac");
                mDialog.setCanceledOnTouchOutside(false);
                mDialog.show();

                //Aktualizacja avatary uztkownika
                //Czytamy stream z pliku i wrzucamy na server
                try {
                    InputStream in = getContentResolver().openInputStream(selectedImageUri);
                    final Bitmap bitmap = BitmapFactory.decodeStream(in);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,bos);
                    File file = new File(Environment.getExternalStorageDirectory()+"/myimage.png");
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(bos.toByteArray());
                    fos.flush();
                    fos.close();

                    //Pobieramy rozmiar pliku
                    final int imageSizeKb = (int)file.length() / 1024;
                    if(imageSizeKb >= (1024*100))
                    {
                        Toast.makeText(this, "Error Za duzy rozmiar", Toast.LENGTH_SHORT).show();

                    }

                    //Wrzucamy obrazek na server
                    QBContent.uploadFileTask(file,true,null)
                            .performAsync(new QBEntityCallback<QBFile>() {
                                @Override
                                public void onSuccess(QBFile qbFile, Bundle bundle) {
                                    //Obrazek na serverze wiec go ustawiamy dla uzytkownika
                                    QBUser user = new QBUser();
                                    user.setId(QBChatService.getInstance().getUser().getId());
                                    user.setFileId(Integer.parseInt(qbFile.getId().toString()));

                                    //Uaktualniamy uzytkownika
                                    QBUsers.updateUser(user)
                                            .performAsync(new QBEntityCallback<QBUser>() {
                                                @Override
                                                public void onSuccess(QBUser qbUser, Bundle bundle) {
                                                    mDialog.dismiss();
                                                    user_avatar.setImageBitmap(bitmap);
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

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }
    }

    private void loadUserProfile() {


        //Ladowanie obrazka uzytkownika
        QBUsers.getUser(QBChatService.getInstance().getUser().getId())
                .performAsync(new QBEntityCallback<QBUser>() {
                    @Override
                    public void onSuccess(QBUser qbUser, Bundle bundle) {
                        //Zapisujemy w cache
                        QBUsersHolder.getInstance().putUser(qbUser);
                        if(qbUser.getFileId() != null)
                        {
                            int profilePictureId = qbUser.getFileId();

                            QBContent.getFile(profilePictureId)
                                    .performAsync(new QBEntityCallback<QBFile>() {
                                        @Override
                                        public void onSuccess(QBFile qbFile, Bundle bundle) {
                                            //Udalo sie pobrac obrazek wiec go ustawiamy
                                            String fileUrl = qbFile.getPrivateUrl();
                                            Picasso.with(getBaseContext())
                                                    .load(fileUrl)
                                                    .into(user_avatar);
                                        }

                                        @Override
                                        public void onError(QBResponseException e) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onError(QBResponseException e) {

                    }
                });




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

        user_avatar = (ImageView)findViewById(R.id.user_avatar);






    }
}

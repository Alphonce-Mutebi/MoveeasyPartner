package com.blume.moveeasypartner;

import java.util.Random;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.blume.moveeasypartner.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    EditText email, pword, username, phone, regNo;
    Button signup;
    ProgressBar progressBar;
    Spinner spinner;
    FirebaseAuth fbAuth;
    TextView tv;
    public String vehicleType;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference mDatabase = database.getInstance().getReference();
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        vehicleType = parent.getItemAtPosition(position).toString();
        Toast.makeText(this, "Vehicle Type: "+vehicleType, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        spinner = findViewById(R.id.spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.vehicle_types, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);


        fbAuth = FirebaseAuth.getInstance();
        email = findViewById(R.id.regEmail);
        pword = findViewById(R.id.regPassword);
        username = findViewById(R.id.username);
        phone = findViewById(R.id.phone);
        regNo = findViewById(R.id.regNumber);
        progressBar = findViewById(R.id.progressBar3);
        signup = findViewById(R.id.regButton);
        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                final String u_name = username.getText().toString();
                final String e_mail = email.getText().toString();
                final String p_word = pword.getText().toString();
                String uphone = phone.getText().toString();
                final int u_phone = Integer.parseInt(uphone);
                final String reg_no = regNo.getText().toString();


                if(e_mail.isEmpty()){
                    email.setError("Fill in email field");
                    email.requestFocus();
                }
                else if(p_word.isEmpty()){
                    pword.setError("Password field is empty");
                    pword.requestFocus();
                }
                else if(p_word.isEmpty() && e_mail.isEmpty()){
                    Toast.makeText(RegisterActivity.this, "Fields are empty!!", Toast.LENGTH_SHORT).show();
                }
                else if (!(p_word.isEmpty() && e_mail.isEmpty())){
                    progressBar.setVisibility(View.VISIBLE);
                    fbAuth.createUserWithEmailAndPassword(e_mail, p_word).addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            progressBar.setVisibility(View.GONE);

                            if(!task.isSuccessful()){
                                Toast.makeText(RegisterActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                            else {

                                User a_user = new User(fbAuth.getUid(), e_mail, u_name, u_phone, reg_no, vehicleType);

                                mDatabase.child("Drivers").child(a_user.get_uid()).child("Email").setValue(a_user.get_email());
                                mDatabase.child("Drivers").child(a_user.get_uid()).child("Username").setValue(a_user.get_uname());
                                mDatabase.child("Drivers").child(a_user.get_uid()).child("Phone").setValue(a_user.get_phone());
                                mDatabase.child("Drivers").child(a_user.get_uid()).child("Reg_no").setValue(a_user.get_reg_no());
                                mDatabase.child("Drivers").child(a_user.get_uid()).child("Vehicle").setValue(a_user.get_vehicleType());

                                FirebaseUser user = fbAuth.getCurrentUser();
                                user.sendEmailVerification()
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(RegisterActivity.this, "Verification sent via email", Toast.LENGTH_LONG).show();

                                                    Intent toLogin = new Intent(RegisterActivity.this, MainActivity.class);
                                                    startActivity(toLogin);
                                                }
                                            }
                                        });
                            }
                        }
                    });

                }
                else {
                    Toast.makeText(RegisterActivity.this, "An error ccured, Please try again in a few...", Toast.LENGTH_LONG).show();
                }

            }
        });
    }


}

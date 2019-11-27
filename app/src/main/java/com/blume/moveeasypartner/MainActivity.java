package com.blume.moveeasypartner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    EditText email, pword;
    Button signin;
    TextView pwordreset, signup;
    ProgressBar progressBar;

    FirebaseAuth mfbAuth;
    FirebaseAuth.AuthStateListener mAuthStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mfbAuth = FirebaseAuth.getInstance();
        email = findViewById(R.id.Email);
        pword = findViewById(R.id.password);
        signin = findViewById(R.id.login);
        signup = findViewById(R.id.register);
        pwordreset = findViewById(R.id.resetPassword);
        progressBar = findViewById(R.id.progressBar1);

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser mfbUser = mfbAuth.getCurrentUser();

                if(mfbUser != null && mfbUser.isEmailVerified()){
                    Toast.makeText(MainActivity.this, "Welcome Back...", Toast.LENGTH_SHORT).show();
                    Intent ToHome = new Intent(MainActivity.this, MapsActivity.class);
                    startActivity(ToHome);
                }
            }
        };


        signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String e_mail = email.getText().toString();
                String p_word = pword.getText().toString();

                if(e_mail.isEmpty()){
                    email.setError("Fill in email field");
                    email.requestFocus();
                }
                else if(p_word.isEmpty()){
                    pword.setError("Password field is empty");
                    pword.requestFocus();
                }
                else if(p_word.isEmpty() && e_mail.isEmpty()){
                    Toast.makeText(MainActivity.this, "Fields are empty!!", Toast.LENGTH_SHORT).show();
                }

                else if (!(p_word.isEmpty() && e_mail.isEmpty())){
                    mfbAuth.signInWithEmailAndPassword(e_mail, p_word).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){
                                if(mfbAuth.getCurrentUser().isEmailVerified()){
                                    Toast.makeText(MainActivity.this, "Sign-in Approved!! ;) ", Toast.LENGTH_LONG).show();

                                    Intent intToHome = new Intent(MainActivity.this, MapsActivity.class);
                                    startActivity(intToHome);
                                }
                                else{
                                    Toast.makeText(MainActivity.this, "Please Verify your email to sign-in", Toast.LENGTH_LONG).show();
                                }

                            }
                            else{
                                Toast.makeText(MainActivity.this, "Authentication Failed :( \nPlease confirm your credentials and try again...", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
                else {
                    Toast.makeText(MainActivity.this, "An error Occured, Please try again in a few moments", Toast.LENGTH_LONG).show();
                }

            }
        });

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(i);
                //FirebaseAuth.getInstance().signOut();
                //Toast.makeText(MainActivity.this, "Signed out", Toast.LENGTH_SHORT).show();

            }
        });

        pwordreset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                String Email = email.getText().toString();
                if(Email.isEmpty()){
                    email.setError("Enter password reset email here");
                    email.requestFocus();
                }
                else if(!Email.isEmpty()) {
                    mfbAuth.sendPasswordResetEmail(Email)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    progressBar.setVisibility(View.GONE);
                                    if (task.isSuccessful()) {
                                        Toast.makeText(MainActivity.this, "Password reset Email sent \nCheck your email", Toast.LENGTH_LONG).show();
                                    }
                                    else{
                                        Toast.makeText(MainActivity.this, "Password reset email error", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        mfbAuth.addAuthStateListener(mAuthStateListener);
    }
}

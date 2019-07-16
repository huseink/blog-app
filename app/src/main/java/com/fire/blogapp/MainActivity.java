package com.fire.blogapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class MainActivity extends AppCompatActivity {

    private Toolbar mainToolbar;

    private FloatingImageButton newPostBtn;
    private String current_user_id;
    private FirebaseAuth mAuth;


    private FirebaseFirestore firebaseFirestore;

    private RecyclerView blog_list_view;
    private BlogRecyclerAdapter blogRecyclerAdapter;
    private List<BlogPost> blog_list;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // Recycler View and Adapter(s) initialization
        blog_list =  new ArrayList<>();
        blog_list_view = findViewById(R.id.blog_list_view);
        blogRecyclerAdapter = new BlogRecyclerAdapter(blog_list);
        // LinearLayoutManager is used to display our data as a linear vertical list.
        blog_list_view.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        // Attaching blogRecyclerAdapter to the RecyclerView.
        blog_list_view.setAdapter(blogRecyclerAdapter);


        mainToolbar = (Toolbar) findViewById(R.id.main_toolbar);

        setSupportActionBar(mainToolbar);
        getSupportActionBar().setTitle("Blog App");
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        newPostBtn = findViewById(R.id.new_post_btn);


        newPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    Intent postIntent = new Intent(MainActivity.this,NewPostActivity.class);
                    startActivity(postIntent);
            }
        });

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if(currentUser != null){

            firebaseFirestore = FirebaseFirestore.getInstance();

            Query firstQuery = firebaseFirestore.collection("Posts").orderBy("timestamp",Query.Direction.DESCENDING);

            firstQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot documentSnapshots, @Nullable FirebaseFirestoreException e) {

                    if(!(documentSnapshots == null)){
                        for (DocumentChange doc : documentSnapshots.getDocumentChanges()) {

                            if(doc.getType() ==  DocumentChange.Type.ADDED){

                                BlogPost blogPost = doc.getDocument().toObject(BlogPost.class);
                                blog_list.add(blogPost);
                                blogRecyclerAdapter.notifyDataSetChanged();
                            }
                        }

                    }

                }
            });

        }

    }


    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser == null){

            sendToLogin();

        } else {

            current_user_id = mAuth.getCurrentUser().getUid();

            firebaseFirestore.collection("Users").document(current_user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                    if(task.isSuccessful()){
                    // If user's profile image and user_name don't exist in the collection
                        // Redirect him to the account setup page.
                        if(!task.getResult().exists()){

                            Intent setupIntent = new Intent(MainActivity.this, SetupActivity.class);
                            startActivity(setupIntent);
                            finish();

                        }

                    } else {

                        String errorMessage = task.getException().getMessage();
                        Toast.makeText(MainActivity.this, "Error : " + errorMessage, Toast.LENGTH_LONG).show();


                    }

                }
            });

        }

    }




    // Creates the hamburger menu on Toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        System.out.println("menu created");
        getMenuInflater().inflate(R.menu.main_menu,menu);

        return true;
    }

    // What to do when any of the menu items are selected
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){
            case R.id.action_logout_btn:
                logOut();
                return true;
            case R.id.action_settings_btn:
                Intent settingsIntent = new Intent(MainActivity.this,SetupActivity.class);
                startActivity(settingsIntent);
                return true;

                default:
                    return false;
        }

    }

    private void logOut() {

        mAuth.signOut();
        sendToLogin();
    }



    private void sendToLogin() {
        Intent intent = new Intent(MainActivity.this,LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
